package com.example.auth;


import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Authentication
 */
@Path("/auth")
public class AuthRestService {
	private static Logger logger = LoggerFactory.getLogger(AuthRestService.class);

	public static final String X_AUTH_USER = "X-Auth-User";
	public static final String X_AUTH_PASS = "X-Auth-Pass";
	public static final String X_AUTH_TOKEN = "X-Auth-Token";

	public static final String PROP_AUTH_SESSION = "auth-session";
	
	private static final String AUTH_SESSION_TIMEOUT = "AUTH_SESSION_TIMEOUT";// min
	private static final long AUTH_SESSION_TIMEOUT_DEFAULT = 20;// min
	
	public static final String AUTH_CLASS_LDAP = "com.example.auth.LDAPAuthenticator";
	public static final String AUTH_CLASS_DUMMY = "com.example.auth.DummyAuthenticator";
	public static final String AUTH_CLASS = "AUTH_CLASS";

	public static final String STATE_SERVICE_URL = "STATE_SERVICE_URL";
	public static final String STATE_SERVICE_URL_DEFAULT = "http://localhost:8088/api/";

	public static Authenticator authenticator;
	public static String stateServiceURL;
	
	private static long sessionTimeout = AUTH_SESSION_TIMEOUT_DEFAULT; 

	// セッションストア
	private static Hashtable<String, Session> sessions = new Hashtable<String, Session>();

	public static final String X_CLIENT_NAME = "X-CLIENT-NAME";
	public static String hostName;

	static{
		try{
			hostName = System.getenv("HOSTNAME");
			if(null == hostName){
				hostName = InetAddress.getLocalHost().getHostName();
			}
			String s = System.getenv(AUTH_SESSION_TIMEOUT);
			if(null != s){
				sessionTimeout = Long.parseLong(s);
			}
		}catch(Exception e){}
		logger.info("Hostname: " + hostName);
		logger.info("Session timeout: " + sessionTimeout);

		String authClass = null;
		if(null == authClass)	authClass = System.getProperty(AUTH_CLASS);
		if(null == authClass)	authClass = System.getenv(AUTH_CLASS);
		if(null == authClass)	authClass = DummyAuthenticator.class.getName();
		try {
			authenticator = (Authenticator)Class.forName(authClass).newInstance();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		logger.info("Authenticator: " + authenticator.getClass().getName());

		if(null == stateServiceURL) stateServiceURL = System.getenv(STATE_SERVICE_URL);
		if(null == stateServiceURL)	stateServiceURL = System.getProperty(STATE_SERVICE_URL);
		if(null == stateServiceURL)	stateServiceURL = STATE_SERVICE_URL_DEFAULT;
		logger.info("stateServiceURL: " + stateServiceURL);
		
	}
	
/*
	public static synchronized Authenticator getAuthenticator() throws Exception{
		if(null == authenticator){
			String authClass = null;
			if(null == authClass)	authClass = System.getProperty(AUTH_CLASS);
			if(null == authClass)	authClass = System.getenv(AUTH_CLASS);
			if(null == authClass)	authClass = DummyAuthenticator.class.getName();
			authenticator = (Authenticator)Class.forName(authClass).newInstance();
			logger.info("Authenticator: " + authenticator.getClass().getName());
		}
		return authenticator;
	}
	
	public static synchronized String getSateServiceURL(){
		if(null == stateServiceURL){
			if(null == stateServiceURL) stateServiceURL = System.getenv(STATE_SERVICE_URL);
			if(null == stateServiceURL)	stateServiceURL = System.getProperty(STATE_SERVICE_URL);
			if(null == stateServiceURL)	stateServiceURL = STATE_SERVICE_URL_DEFAULT;
			logger.info("stateServiceURL: " + stateServiceURL);
		}
		return stateServiceURL;
	}
*/	
	
	
	/**
	 * Method handling HTTP GET requests. The returned object will be sent to
	 * the client as "text/plain" media type.
	 *
	 * @return String that will be returned as a text/plain response.
	 */
	@GET
	@Path("/in")
	@Produces(MediaType.APPLICATION_JSON)
	public Response authenticate(@Context HttpHeaders hh) {
		try{
//			Authenticator authenticator = getAuthenticator();
			String user = hh.getHeaderString(X_AUTH_USER);
			String pass = hh.getHeaderString(X_AUTH_PASS);
			Map<String, String> id = authenticator.authenticate(user, pass);
			String token = createSession(id.get(Authenticator.IDENTITY), user);
			Session session = getSession(token);
			logger.info("Logged in: " + session);
			UserInfo userInfo = new UserInfo(id.get(Authenticator.USERNAME));
			return Response.ok().header("X-Auth-Token", token).entity(userInfo).build();
		}catch(Exception e){
			logger.info("Authentication failed: " + e.getMessage(), e);
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
	}

	@GET @Auth
	@Path("/myinfo")
	@Produces(MediaType.APPLICATION_JSON)
	public Session getInfo(@Context ContainerRequestContext crc) {
		logger.debug("Principal: " + crc.getSecurityContext().getUserPrincipal().getName());
		Session session = (Session)crc.getProperty(PROP_AUTH_SESSION);
		return session;
	}

	@GET @Auth
	@Path("/out")
	public Response logout(@HeaderParam(X_AUTH_TOKEN) String token){
		Session session = removeSession(token);
		if(null == session){
			logger.warn("No session found for token: " + token);
		}
		logger.info("Logged out: " + token);
		return Response.ok().build();
	}
	
	//////////////////////////////////////////////////////////////////
	
	public static String createSession(String id, String username){
		synchronized(sessions){
			// 既に認証済みか？ - ローカルキャッシュを確認
			/* 
			// 同じidを検索すると違うブラウザからのアクセスもログイン済という風に
			// みなしてしまうのでこの検査はしない
			for(String key : sessions.keySet()){
				//System.out.println("Key: " + key);
				Session session = sessions.get(key);
				//System.out.println(id + " : " + session.id);
				if(id.equalsIgnoreCase(session.id)){
					session.authTime = System.currentTimeMillis();
					logger.info("Session renewed: " + session.id);
					// リモートも更新
					stateServicePost(key, session);
					return key;
				}
			}
			*/
			
			// 新しいsessionを作成
			UUID uuid = UUID.randomUUID();
			String authToken = uuid.toString();
			Session session = new Session(id, username);
			sessions.put(authToken, session);
			logger.info("Session created - id: " + session.id + ", authToken: " + authToken);

			// リモートも更新
			stateServicePost(authToken, session);
			return authToken;
		}
	}
	
	public static Session getSession(String authToken){
		Session session = sessions.get(authToken);
		// キャッシュに見つからなかったらリモートを確認
		if(null == session){
			session = stateServiceGet(authToken);
			if(null != session){ // ローカルキャッシュに入れる
				sessions.put(authToken, session);
			}
		}
		return session;
	}
	
	public static Session removeSession(String authToken){
		Session session = null;
		synchronized(sessions){
			session = sessions.remove(authToken);
		}
		// リモートも消去
		session = stateServiceDelete(authToken);
		return session;
	}

	public static Session validateSession(String authToken) throws Exception{
		// まずローカルキャッシュを調べる
		Session session = sessions.get(authToken);
		if(null == session){ // ローカルキャッシュにない場合
			session = stateServiceGet(authToken);
			if(null == session){ // リモートにもない
				throw new Exception("No session: " + authToken);
			}
		}else{ // ローカルキャッシュはありました。
			// タイムアウトしていないか確認
			if((System.currentTimeMillis() - session.authTime)/1000 > sessionTimeout * 60){ // ローカルがタイムアウト
				synchronized (sessions) { // お掃除
					sessions.remove(authToken);
				}
				// ローカルキャッシュがタイムアウトしていたらリモートが更新されているか確認
				session = stateServiceGet(authToken);
				if((System.currentTimeMillis() - session.authTime)/1000 > sessionTimeout * 60){ // リモートもタイムアウト
					stateServiceDelete(authToken); // リモートもお掃除
					throw new Exception("Session timeout - " + session);
				}
			}
			
		}

		// いずれの場合もローカル・リモートとも更新しておく
		session.authTime = System.currentTimeMillis();
		synchronized(sessions){
			sessions.put(authToken, session);
		}
		stateServicePost(authToken, session);
		return session;
	}
	
	
	//////////////////////////////////////////////////////////////////
	
	private static void stateServicePost(String authToken, Session session){
        Client c = ClientBuilder.newClient();
        try{
        	
            WebTarget target = c.target(stateServiceURL);
/*
    		List<KeyValue> list = new ArrayList<KeyValue>();
    		list.add(new KeyValue("id", session.id));
    		list.add(new KeyValue("username", session.username));
    		list.add(new KeyValue("authTime", new Long(session.authTime).toString()));
            
            Response response = target.path("state").path(authToken)
            		.request()
            		.header(X_CLIENT_NAME, hostName)
            		.post(Entity.entity(list.toArray(new KeyValue[list.size()]), MediaType.APPLICATION_JSON_TYPE));
            logger.info("POST: " + response.getStatus() + " - authToken=" + authToken + ", Id=" + session.id);
*/
    		
            JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
			jsonArrayBuilder.add(Json.createObjectBuilder().add("key", "id").add("value", session.id));
			jsonArrayBuilder.add(Json.createObjectBuilder().add("key", "username").add("value", session.username));
			jsonArrayBuilder.add(Json.createObjectBuilder().add("key", "authTime").add("value", new Long(session.authTime).toString()));
			JsonArray array = jsonArrayBuilder.build();
			//logger.info(array.toString());

			Response response = target.path("state").path(authToken)
            		.request()
            		.header(X_CLIENT_NAME, hostName)
            		.post(Entity.entity(array.toString(), MediaType.APPLICATION_JSON_TYPE));
            logger.info("POST: " + response.getStatus() + " - authToken=" + authToken + ", Id=" + session.id);

            //logger.info(response.getStatusInfo().getReasonPhrase());
            
		}finally{
        	if(null != c)	c.close();
        }
	}

	private static Session stateServiceGet(String authToken){
        Client c = ClientBuilder.newClient();
        try{
//    		WebTarget target = c.target(getSateServiceURL());
    		WebTarget target = c.target(stateServiceURL);
            
    		Response response = target.path("state").path(authToken)
            		.request(MediaType.APPLICATION_JSON_TYPE)
            		.header(X_CLIENT_NAME, hostName)
            		.get();
            logger.info("GET: " + response.getStatus() + " - authToken=" + authToken);
            KeyValue[] items = response.readEntity(KeyValue[].class);
            if(null == items){
            	return null;
            }
            HashMap<String, String> map = new HashMap<String, String>();
            for(KeyValue item : items){
            	map.put(item.getKey(), item.getValue());
            }
            Session session = new Session(map.get("id"), map.get("username"), Long.parseLong(map.get("authTime")));
        	return session;
        }finally{
        	if(null != c)	c.close();
        }
	}

	private static Session stateServiceDelete(String authToken){
        Client c = ClientBuilder.newClient();
        try{
//    		WebTarget target = c.target(getSateServiceURL());
    		WebTarget target = c.target(stateServiceURL);
            
    		Response response = target.path("state").path(authToken)
            		.request(MediaType.APPLICATION_JSON_TYPE)
            		.header(X_CLIENT_NAME, hostName)
            		.delete();
            logger.info("DELETE: " + response.getStatus() + " - authToken=" + authToken);
            KeyValue[] items = response.readEntity(KeyValue[].class);
            if(null == items){
            	return null;
            }
            HashMap<String, String> map = new HashMap<String, String>();
            for(KeyValue item : items){
            	map.put(item.getKey(), item.getValue());
            }
            return new Session(map.get("id"), map.get("usernrmae"), Long.parseLong(map.get("authTime")));

        }finally{
        	if(null != c)	c.close();
        }
	}
	
	
	/**
	 * セッションを管理する構造体
	 * 
	 * */
	public static class Session{
		public String username;
		public String id;
		public long authTime;
		
		public Session(String id, String username){
			this.username = username;
			this.id = id;
			this.authTime = System.currentTimeMillis();
		}
		public Session(String id, String username, long authTime){
			this.username = username;
			this.id = id;
			this.authTime = authTime;
		}

		public String toString(){
			return "[" + username + "][" + id + "][" + authTime + "]";
		}
		
	}
	
	/**
	 * keyvalののコンテナ
	 * */
	public static class KeyValue{
		
		private String key;
		private String value;
		
		public KeyValue(){}

		public KeyValue(String key, String value){
			this.key = key;
			this.value = value;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
		
		public String getJsonString(){
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
	        pw.printf("{ \"key\" : \"%s\", \"value\" : \"%s\" }", key, value);
	        pw.flush();
	        return sw.toString();
		}
		
		
	}

	/* ユーザー情報 */
	public static class UserInfo{
		public String username;
		
		public UserInfo(){}
		public UserInfo(String s){
			username = s;
		}
		
	}
	
	
	////////////////////
	
	public static void main(String[] args){
//		Session session = new Session("ID", "USERNAME");
//		stateServicePost("TOKEN_" + System.currentTimeMillis(), session);

        Client c = ClientBuilder.newClient();
        	
            WebTarget target = c.target(stateServiceURL);

            Response response = target.path("state").path("TOKEN_" + System.currentTimeMillis())
            		.request()
            		.header(X_CLIENT_NAME, hostName)
            		.post(Entity.entity("[{\"key\" : \"KEY1\", \"value\" : \"VAL1\"},{\"key\" : \"KEY1\", \"value\" : \"VAL1\"}]", MediaType.APPLICATION_JSON_TYPE));
            logger.info("POST: " + response.getStatus());// + " - authToken=" + authToken + ", Id=" + session.id);
	
	}
	
}


