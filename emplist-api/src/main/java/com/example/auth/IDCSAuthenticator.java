package com.example.auth;


import java.io.StringReader;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Authentication
 * Identity Cloud Serviceを使った認証処理
 */
public class IDCSAuthenticator implements Authenticator{
	private Logger logger = LoggerFactory.getLogger(getClass());

	private static final String AUTH_IDCS_URL = "AUTH_IDCS_URL";
	private static final String AUTH_IDCS_HOST = "AUTH_IDCS_HOST";
	private static final String AUTH_IDCS_CLIENT_ID = "AUTH_IDCS_CLIENT_ID";
	private static final String AUTH_IDCS_CLIENT_SECRET = "AUTH_IDCS_CLIENT_SECRET";
	
	private String clientID;// = "2739801448e54ddd9e01de79e1f3061f";
	private String clientSecret;// = "17de1b15-ffe1-4b35-b1a2-de9b0a0477b5";
	private String idcsURL;// = "https://xxxxxx.oracle.com:8943";
	private String idcsHost;

	private static final String API_TOKEN = "/oauth2/v1/token";
	private static final String API_PWAUTH = "/admin/v1/PasswordAuthenticator";
	
	private AccessTokenInfo token;
	private long lastFetched;

	static{
		System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
	}
	
	public IDCSAuthenticator() throws Exception{
		idcsURL = System.getenv(AUTH_IDCS_URL);
		idcsHost = System.getenv(AUTH_IDCS_HOST);
		clientID = System.getenv(AUTH_IDCS_CLIENT_ID);
		clientSecret = System.getenv(AUTH_IDCS_CLIENT_SECRET);

		logger.info("AUTH_IDCS_URL: " + idcsURL);
		logger.info("AUTH_IDCS_HOST: " + idcsHost);
		logger.info("AUTH_IDCS_CLIENT_ID: " + clientID);
		logger.info("AUTH_IDCS_CLIENT_SECRET: " + clientSecret);
		
		if(null == idcsURL || null == clientID || null == clientSecret){
			return;
		}

		/* 最初にトークンを取っていっておく*/
        Client c = ForJaxRsClient.getLooseSslClient();
		try{
			getAccessToken(c);
		}catch(Exception e){
			logger.warn("Failed to get access token: " + e.getMessage());
		}finally{
			if(null != c) c.close();
		}
	}
	
	
	// アクセストークンを取得
	private void getAccessToken(Client c) throws Exception{
		logger.info("Getting access token.");
		String userPassword = clientID + ":" + clientSecret;
		WebTarget target = c.target(idcsURL).path(API_TOKEN);
		Builder builder = target.request()
							.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(userPassword.getBytes()));
		if(null != idcsHost){
			builder.header("Host", idcsHost);
		}
		Response response = builder.post(Entity.entity("grant_type=client_credentials&scope=urn:opc:idm:__myscopes__", "application/x-www-form-urlencoded;charset=UTF-8"));
				
		if(2 != response.getStatus()/100){
			throw new Exception("(" + response.getStatus() + ")" + response.readEntity(String.class));
		}
		token = response.readEntity(AccessTokenInfo.class);
		lastFetched = System.currentTimeMillis();
	}
	
	/**
	 * IDCSで認証する
	 * */
	@Override
	public Map<String, String> authenticate(String username, String password) throws Exception{

        Client c = ForJaxRsClient.getLooseSslClient();
        try{
        	//logger.debug("token.expires_in: " + (null == token ? null : token.expires_in));
        	//logger.debug("lastFetched: " + lastFetched);
        	//logger.debug("diff: " + (System.currentTimeMillis() - lastFetched)/1000);
        	// アクセストークンを取得する必要あるか？
        	if(null == token){
    			logger.info("Not Authed.");
        		getAccessToken(c);
        	}else{
        		if((System.currentTimeMillis() - lastFetched)/1000 > (token.expires_in - 30/* 30secはバッファ*/)){
        			logger.info("Auth expired.");
            		getAccessToken(c);
        		}
        	}

        	/* PasswordAuthenticator のリクエスト
        	{
        	  "mappingAttribute": "userName",
        	  "mappingAttributeValue": "admin@oracle.com",
        	  "password": "Welc0me@1",
        	  "schemas": [
        	    "urn:ietf:params:scim:schemas:oracle:idcs:PasswordAuthenticator"
        	  ]
        	} 
        	 */    	
        	JsonObjectBuilder json = Json.createObjectBuilder();
			json.add("mappingAttribute", "userName");
			json.add("mappingAttributeValue", username);
			json.add("password", password);
			JsonArrayBuilder schemasArray = Json.createArrayBuilder();
			schemasArray.add("urn:ietf:params:scim:schemas:oracle:idcs:PasswordAuthenticator");
			json.add("schemas", schemasArray);
        	
    		WebTarget target = c.target(idcsURL).path(API_PWAUTH);
    		Builder builder = target.request()
						.header("Authorization", "Bearer " + token.getAccess_token());
			if(null != idcsHost){
				builder.header("Host", idcsHost);
			}
			Response response = builder.post(Entity.entity(json.build().toString(), "application/scim+json"));

    		int code = response.getStatus();
    		if(201 != code){ // エラー
    			throw new Exception("(" + response.getStatus() + ")" + response.readEntity(String.class));
    		}
    		//System.out.println(response.getStatus());
    		//System.out.println(response.readEntity(String.class));
    		
    		/*    		
			201 正常終了時のjsonオブジェクト　- 全てがStringでない（schemasは配列）ので注意
			{
				"id":"f15aa5035a4a42ae84fba6a19799f840",
				"userEmail":"oracle@oracle.com",
				"userDisplayName":"oracle opc",
				"locale":"ja","preferredLanguage":"ja",
				"timezone":"Asia/Tokyo",
				"csr":false,
				"tenantName":"mydemotenant1",
				"type":"User",
				"mappingAttributeValue":"oracle",
				"mappingAttribute":"userName",
				"schemas":["urn:ietf:params:scim:schemas:oracle:idcs:PasswordAuthenticator"]
			}
    		*/    		
    		JsonReader reader = Json.createReader(new StringReader(response.readEntity(String.class)));
    		JsonObject res = reader.readObject();
    		Map<String, String> identity = new HashMap<String, String>();
    		for(String key : res.keySet()){
    			JsonValue val = res.get(key);
    			if(val.getValueType() == ValueType.STRING){
    				identity.put(key, res.getJsonString(key).getString());
    			}else{ // String型でないものは便宜上強制的に文字列にしておく
    				identity.put(key, val.toString());
    	   		}
    		}
    		// Authenticatorの決めごと
    		identity.put(USERNAME, res.getJsonString("userDisplayName").getString());
			identity.put(IDENTITY, res.getJsonString("id").getString());
    		return identity;
    		
        }finally{
        	if(null != c)	c.close();
        }
	}

	public String getClientID() {
		return clientID;
	}
	public void setClientID(String clientID) {
		this.clientID = clientID;
	}
	public String getClientSecret() {
		return clientSecret;
	}
	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}
	public String getIdcsURL() {
		return idcsURL;
	}
	public void setIdcsURL(String idcsURL) {
		this.idcsURL = idcsURL;
	}
	public String getIdcsHost() {
		return idcsHost;
	}
	public void setIdcsHost(String idcsHost) {
		this.idcsHost = idcsHost;
	}

	
    /*    Json BindingのためのPOJO
    Status: 200
    Content: application/json;charset=UTF-8
    Content: {
    	"access_token":"eyJ4NXQjUzI1NiI6Ijg1a3E1MFVBVmNSRDJOUTR6WVZMVDZXbndUZmVidjBhNGV2YUJGMjFqbU0iLCJ4NXQiOiJNMm1hRm0zVllsTUJPbjNHZXRWV0dYa3JLcmsiLCJraWQiOiJTSUdOSU5HX0tFWSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiIyNzM5ODAxNDQ4ZTU0ZGRkOWUwMWRlNzllMWYzMDYxZiIsInVzZXIudGVuYW50Lm5hbWUiOiJteWRlbW90ZW5hbnQxIiwic3ViX21hcHBpbmdhdHRyIjoidXNlck5hbWUiLCJpc3MiOiJodHRwczpcL1wvaWRlbnRpdHkub3JhY2xlY2xvdWQuY29tXC8iLCJ0b2tfdHlwZSI6IkFUIiwiY2xpZW50X2lkIjoiMjczOTgwMTQ0OGU1NGRkZDllMDFkZTc5ZTFmMzA2MWYiLCJhdWQiOiJodHRwczpcL1wvbXlkZW1vdGVuYW50MS5pZGNzLmludGVybmFsLm9yYWNsZS5jb206ODk0MyIsImNsaWVudEFwcFJvbGVzIjpbIlVzZXIgQWRtaW5pc3RyYXRvciIsIkZvcmdvdCBQYXNzd29yZCIsIkF1dGhlbnRpY2F0ZWQgQ2xpZW50IiwiVmVyaWZ5IEVtYWlsIiwiQXVkaXQgQWRtaW5pc3RyYXRvciIsIkNsb3VkIEdhdGUiLCJBcHBsaWNhdGlvbiBBZG1pbmlzdHJhdG9yIiwiUmVzZXQgUGFzc3dvcmQiLCJHbG9iYWwgVmlld2VyIiwiSWRlbnRpdHkgRG9tYWluIEFkbWluaXN0cmF0b3IiLCJNZSIsIlNpZ25pbiIsIkNoYW5nZSBQYXNzd29yZCIsIlNlY3VyaXR5IEFkbWluaXN0cmF0b3IiXSwic2NvcGUiOiJ1cm46b3BjOmlkbTp0Lm9hdXRoIHVybjpvcGM6aWRtOnQuZ3JvdXBzLm1lbWJlcnNfciB1cm46b3BjOmlkbTp0Lmdyb3Vwcy5tZW1iZXJzIHVybjpvcGM6aWRtOnQuYXBwIHVybjpvcGM6aWRtOnQuZ3JvdXBzIHVybjpvcGM6aWRtOnQubmFtZWRhcHBhZG1pbiB1cm46b3BjOmlkbTp0LmdyYW50cyB1cm46b3BjOmlkbTp0LnNlY3VyaXR5LmNsaWVudCB1cm46b3BjOmlkbTp0LnVzZXIuYXV0aGVudGljYXRlIHVybjpvcGM6aWRtOnQuaW1hZ2VzIHVybjpvcGM6aWRtOnQuYnVsayB1cm46b3BjOmlkbTp0LmJ1bGsudXNlciB1cm46b3BjOmlkbTp0LmpvYi5zZWFyY2ggdXJuOm9wYzppZG06dC5kaWFnbm9zdGljc19yIHVybjpvcGM6aWRtOnQuaWRicmlkZ2UgdXJuOm9wYzppZG06dC5pZGJyaWRnZS51c2VyIHVybjpvcGM6aWRtOnQudXNlci5tZSB1cm46b3BjOmlkbTpnLmFsbF9yIHVybjpvcGM6aWRtOnQuaWRicmlkZ2VfciB1cm46b3BjOmlkbTp0LnVzZXIuc2VjdXJpdHkgdXJuOm9wYzppZG06dC51c2VyLnJlc2V0cGFzc3dvcmQgdXJuOm9wYzppZG06dC5ncm91cHNfciB1cm46b3BjOmlkbTp0LnNldHRpbmdzIHVybjpvcGM6aWRtOnQuYXVkaXRfciB1cm46b3BjOmlkbTp0LmpvYi5hcHAgdXJuOm9wYzppZG06dC51c2VyLnZlcmlmeWVtYWlsIHVybjpvcGM6aWRtOnQub2F1dGhjb25zZW50cyB1cm46b3BjOmlkbTp0LnVzZXIuc2lnbmluIHVybjpvcGM6aWRtOnQudXNlcnNfciB1cm46b3BjOmlkbTp0LnVzZXIuY2hhbmdlcGFzc3dvcmQgdXJuOm9wYzppZG06Zy5zaGFyZWRmaWxlcyB1cm46b3BjOmlkbTp0LnVzZXJzIHVybjpvcGM6aWRtOnQucmVwb3J0cyB1cm46b3BjOmlkbTp0LmpvYi5pZGVudGl0eSB1cm46b3BjOmlkbTp0LmVuY3J5cHRpb25rZXkgdXJuOm9wYzppZG06dC5zYW1sIHVybjpvcGM6aWRtOnQudXNlci5mb3Jnb3RwYXNzd29yZCIsImNsaWVudF90ZW5hbnRuYW1lIjoibXlkZW1vdGVuYW50MSIsImV4cCI6MTQ3OTgxMTQ5OCwiaWF0IjoxNDc5ODA3ODk4LCJjbGllbnRfbmFtZSI6Ik15QXBwc1Rvb2xBcHAiLCJ0ZW5hbnQiOiJteWRlbW90ZW5hbnQxIiwianRpIjoiNTZjMzEzNDctZWY4Mi00NDMwLThkMzktZDIzNjMzYTI2MjZmIn0.QrmkTg_BSYkFCHXR_OtfHszcFgeM95qSM05MkycA3gh1IU1c5MNf2EHGWERoNWNHvNhcpkiQj2aEciyzzt1Kq2tEFwZ_Bx6d13z4hcrmVnRvlWTHhs-hFih7a5xhj1fTlDojluOfzLwefMkwMHfJAzvy_c0fColMpRvVu8DOqds",
    	"token_type":"Bearer",
    	"expires_in":3600
    }
    */
    public static class AccessTokenInfo{
    	private String access_token;
    	private String token_type;
    	private long expires_in;
    	
    	public AccessTokenInfo(){}
    	public String getAccess_token() {
			return access_token;
		}
		public void setAccess_token(String access_token) {
			this.access_token = access_token;
		}
		public String getToken_type() {
			return token_type;
		}
		public void setToken_type(String token_type) {
			this.token_type = token_type;
		}
		public long getExpires_in() {
			return expires_in;
		}
		public void setExpires_in(long expires_in) {
			this.expires_in = expires_in;
		}
    }

	public static void main(String[] args) throws Exception{
		
		//System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
		
		//System.setProperty("javax.net.debug", "all");
		IDCSAuthenticator auth = new IDCSAuthenticator();
		auth.setIdcsURL("https://140.86.14.158:8943");
		auth.setIdcsHost("mydemotenant1.idcs.internal.oracle.com:8943");
		auth.setClientID("cc81d23656fb4672a09bede9722b43eb");
		auth.setClientSecret("beec43c2-8525-4f7e-94d6-35d017a2df10");
		
		//try{
		//	auth.authenticate("oracle", "welcome2");
		//}catch(Exception e){e.printStackTrace();}
		//Thread.sleep(3000);
		
		Map<String, String> res = auth.authenticate("oracle", "welcome1");
		for(String key : res.keySet()){
			System.out.println(key + ": " + res.get(key));
		}
		//Thread.sleep(3000);
		//try{
		//	auth.authenticate("oracle", "welcome3");
		//}catch(Exception e){e.printStackTrace();}
	}




	
    
	
	
}


