package com.example.auth;


import java.io.StringReader;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.auth.AuthRestService.KeyValue;
import com.example.auth.AuthRestService.Session;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;


/**
 * Authentication
 * Identity Cloud Serviceを使った認証処理
 */
@Path("/oauth")
public class IDCSOAuthService{
	private static Logger logger = LoggerFactory.getLogger(IDCSOAuthService.class);

	private static final String AUTH_IDCS_URL = "AUTH_IDCS_URL";
	private static final String AUTH_IDCS_HOST = "AUTH_IDCS_HOST";
	private static final String AUTH_IDCS_CLIENT_ID = "AUTH_IDCS_CLIENT_ID";
	private static final String AUTH_IDCS_CLIENT_SECRET = "AUTH_IDCS_CLIENT_SECRET";
	private static final String AUTH_IDCS_CALLBACK_SCHEME = "AUTH_IDCS_CALLBACK_SCHEME";
	
	private String clientID;// = "2739801448e54ddd9e01de79e1f3061f";
	private String clientSecret;// = "17de1b15-ffe1-4b35-b1a2-de9b0a0477b5";
	private String idcsURL;// = "https://xxxxxx.oracle.com:8943";
	private String idcsHost;
	private String idcsCallbackScheme;
	
	private static final String API_TOKEN = "/oauth2/v1/token";
	private static final String API_AUTHORIZE = "/oauth2/v1/authorize";
	private static final String API_USERINFO = "/oauth2/v1/userinfo";
	private static final String API_USERLOGOUT = "/oauth2/v1/userlogout";
	private static final String API_PWAUTH = "/admin/v1/PasswordAuthenticator";

//	private AccessTokenInfo token;
//	private long lastFetched;

	static{
		System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
	}
	
	public IDCSOAuthService() throws Exception{
		idcsURL = System.getenv(AUTH_IDCS_URL);
		idcsHost = System.getenv(AUTH_IDCS_HOST);
		clientID = System.getenv(AUTH_IDCS_CLIENT_ID);
		clientSecret = System.getenv(AUTH_IDCS_CLIENT_SECRET);
		idcsCallbackScheme = System.getenv(AUTH_IDCS_CALLBACK_SCHEME);

		logger.info("AUTH_IDCS_URL: " + idcsURL);
		logger.info("AUTH_IDCS_HOST: " + idcsHost);
		logger.info("AUTH_IDCS_CLIENT_ID: " + clientID);
		logger.info("AUTH_IDCS_CLIENT_SECRET: " + clientSecret);
		logger.info("AUTH_IDCS_CALLBACK_SCHEME: " + idcsCallbackScheme);
		
		if(null == idcsURL || null == clientID || null == clientSecret){
			return;
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

	
    /**    Json BindingのためのPOJO
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
    	private String id_token;
    	
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
		public String getId_token() {
			return id_token;
		}
		public void setId_token(String id_token) {
			this.id_token = id_token;
		}
    }

    /**
     * SPAからキックされる認証処理のスタートポイント
     * redirect_uriを通してあとから/returnがコールされる
     * */
	@GET
	@Path("/start")
	@Produces(MediaType.APPLICATION_JSON)
	public Response oauthStart(@Context UriInfo uriInfo) {

/*
Generate Authorization Code and Identity Token (3-legged OAuth Flow)
https://docs.oracle.com/cloud/latest/identity-cloud/IDCSA/op-oauth2-v1-authorize-get.html#examples


		https://mydemotenant1.idcs.internal.oracle.com:8943/oauth2/v1/authorize?
		client_id=cc81d23656fb4672a09bede9722b43eb&
		response_type=code&
		scope=openid&
		redirect_uri=http://localhost:8080/cquotes/return&
		state=1234
*/		
		
		
//		try{
			// redirect uriとstateを決める
			// ACCSだとUriInfはhttpになっているので注意
			String state = UUID.randomUUID().toString();
			logger.info("state: " + state);
			//String redirect_uri = uriInfo.getBaseUri().toString().concat("oauth/return");
			String redirect_uri = (null != idcsCallbackScheme) ?
					uriInfo.getBaseUriBuilder().scheme(idcsCallbackScheme).path("oauth/return").build().toString()
					: uriInfo.getBaseUriBuilder().path("oauth/return").build().toString();
			logger.info("redirect_uri: " + redirect_uri);
			
			StringBuffer buf = new StringBuffer();
			buf.append("https://" + idcsHost).append(API_AUTHORIZE).append("?");
			buf.append("client_id=").append(clientID).append("&");
			buf.append("response_type=code&");
			buf.append("scope=openid&");
			buf.append("redirect_uri=").append(redirect_uri).append("&");
			buf.append("state=").append(state);
			buf.append("&").append("nonce=").append(UUID.randomUUID().toString());
			String call_uri = buf.toString();
			logger.info("call_uri: " + call_uri);
		
	    	JsonObjectBuilder json = Json.createObjectBuilder();

	    	json.add("call_uri", call_uri);
			json.add("state", state);
			
			return Response.ok().entity(json.build().toString()).build();

//		}catch(Exception e){
//			logger.info(e.getMessage(), e);
//			return Response.status(Status.UNAUTHORIZED).build();
//		}
		
	}

    /**
     * SPAからキックされる認証の継続処理
     * /returnがコールされる（クラスタリングされている場合別サーバーの可能性あり）まで待ち合せる
     * */
	@GET
	@Path("/result")
	@Produces(MediaType.APPLICATION_JSON)
	public Response oauthResult(@QueryParam(value="state") String state) {
		logger.info("/result called, state: " + state);
		
		try{
			// とりあえず60秒待ち合わせ
			String code = null;;
			for(int i = 0 ; i < 60 ; i++){
				Thread.sleep(1000);
				code = StateService.get(state);
				if(null != code){
					break;
				}
			}
			if(null == code){
				throw new Exception("timeout.");
			}

			// アクセストークンを取りに行く
/*
Authorization Code Grant Type (3-Legged Flow) 
https://docs.oracle.com/cloud/latest/identity-cloud/IDCSA/op-oauth2-v1-token-post.html#GUID-51E5C29A-6B7E-487A-8832-5D709410C16A__AuthorizationCodeGrantType3-LeggedF-29E1D686

POST /oauth2/v1/token HTTP/1.1
Host: mydemotenant1.idcs.internal.oracle.com:8943
Content-Type: application/x-www-form-urlencoded;charset=UTF-8
Authorization: Basic <base 64 encoded cliend_id:client_secret>
Accept: * / *

grant_type=authorization_code&code=<Authorization Code>
*/
			logger.info("Getting access token.");
			String userPassword = clientID + ":" + clientSecret;
	        Client c = ForJaxRsClient.getLooseSslClient();
			WebTarget target = c.target(idcsURL).path(API_TOKEN);
			Builder builder = target.request()
								.accept("*/*")
								.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(userPassword.getBytes()));
			if(null != idcsHost){
				builder.header("Host", idcsHost);
			}
			Response response = builder.post(Entity.entity(
					"grant_type=authorization_code&code=" + URLEncoder.encode(code, "UTF-8"),
					"application/x-www-form-urlencoded;charset=UTF-8"));
			
			if(2 != response.getStatus()/100){
				throw new Exception("(" + response.getStatus() + ")" + response.readEntity(String.class));
			}
			AccessTokenInfo token = response.readEntity(AccessTokenInfo.class);
			logger.info("access_token: " + token.access_token);
			logger.info("id_token: " + token.id_token);
			
			// tokenを調べてみる
			checkToken(token);
			
			// ユーザー情報を取りに行く
/*
Generate User Details Using Access Token
https://docs.oracle.com/cloud/latest/identity-cloud/IDCSA/op-oauth2-v1-userinfo-get.html#examples

GET /oauth2/v1/userinfo?access_token=<token>
Authorization: Bearer <token>
*/			
			logger.info("Getting user info.");
			target = c.target(idcsURL).path(API_USERINFO).queryParam("access_token", URLEncoder.encode(token.access_token, "UTF-8"));
			builder = target.request()
								.header("Authorization", "Bearer " + URLEncoder.encode(token.access_token, "UTF-8"));
			if(null != idcsHost){
				builder.header("Host", idcsHost);
			}
			response = builder.get();
			
			if(2 != response.getStatus()/100){
				throw new Exception("(" + response.getStatus() + ")" + response.readEntity(String.class));
			}
			
/*
{
  "birthdate": "",
  "family_name": "Kotegawa",
  "gender": "",
  "given_name": "Tadahisa",
  "name": "Tadahisa Kotegawa",
  "preferred_username": "tadahisa.kotegawa@oracle.com",
  "sub": "tadahisa.kotegawa@oracle.com",
  "website": ""
}
*/
    		String entity = response.readEntity(String.class);
			logger.info(entity);
			JsonReader reader = Json.createReader(new StringReader(entity));
    		JsonObject res = reader.readObject();

			String my_token = AuthRestService.createSession(token.id_token, res.getString("name"));
    		
			return Response.ok(entity).header("X-Auth-Token", my_token).build();
		
		}catch(Exception e){
			logger.info("Authorization failed: " + e.getMessage(), e);
			return Response.status(Status.UNAUTHORIZED).build();
		}finally{
			try{
				StateService.delete(state);
			}catch(Exception e){
				logger.error("Coundn't remove remote state: " + e.getMessage(), e);
			}
		}
		
	}

	/**
	 * 認証のコールバック先
	 * 認証コードをstate service経由で待ち合わせ先に知らせてあげる
	 * */
	@GET
	@Path("/return")
	//@Produces(MediaType.APPLICATION_JSON)
	public Response oauthCallback(@Context UriInfo uriInfo, 
			@QueryParam(value="code") String code, @QueryParam(value="state") String state) {
		logger.info("/return called");
		logger.info("code: " + code);
		logger.info("state: " + state);
		
		try{
			StateService.put(state, code);
		}catch(Exception e){
			logger.error("Coundn't update remote state: " + e.getMessage(), e);
		}
		return Response.ok().entity("認証中...").build();
	
	}

	/**
	 * SPAからコールされるログアウトの開始ポイント
	 * */
	@GET
	@Path("/logout_start")
	@Produces(MediaType.APPLICATION_JSON)
	public Response oauthLogoutStart(@Context UriInfo uriInfo, @HeaderParam(AuthRestService.X_AUTH_TOKEN) String token) throws Exception{
		logger.info("/logout_start called");
		Session session = AuthRestService.removeSession(token);
		if(null == session){
			logger.warn("No session found for token: " + token);
			throw new Exception("No Session");
		}

/*
https://docs.oracle.com/cloud/latest/identity-cloud/IDCSA/op-oauth2-v1-userlogout-get.html

		/oauth2/v1/userlogout?
		post_logout_redirect_uri=
		&id_token_hint=<id_token>
		&state=<state>
*/
		// ACCSだとUriInfはhttpになっているので注意
		String redirectUri = (null != idcsCallbackScheme) ?
				uriInfo.getBaseUriBuilder().scheme(idcsCallbackScheme).path("oauth/post_logout").build().toString() :
				uriInfo.getBaseUriBuilder().path("oauth/post_logout").build().toString();
		logger.info("post_logout_redirect_uri: " + redirectUri);

		String state = UUID.randomUUID().toString();
		UriBuilder builder = UriBuilder.fromPath("https://" + idcsHost).path(API_USERLOGOUT)
				.queryParam("state", state)
				.queryParam("id_token_hint", URLEncoder.encode(session.id, "UTF-8"))
				//.queryParam("post_logout_redirect_uri", uriInfo.getBaseUri().toString().concat("oauth/finish"))
				.queryParam("post_logout_redirect_uri", redirectUri);
    	String path = builder.toString();
    	logger.info("logout uri: " + path);

    	JsonObjectBuilder json = Json.createObjectBuilder();
    	json.add("call_uri", path);
    	json.add("state", state);
		
		return Response.ok().entity(json.build().toString()).build();
	}
	
	@GET
	@Path("/error")
	//@Produces(MediaType.APPLICATION_JSON)
	public void oauthError(@Context UriInfo uriInfo) {
		logger.info("/error called");
		
	}

	/**
	 * /post_logoutの前に呼ばれる
	 * セッションのinvalidateとかはここで処理する
	 * */
	@GET
	@Path("/logout")
	//@Produces(MediaType.TEXT_PLAIN)
	public Response oauthLogout() {
		logger.info("/logout called");
		return Response.ok().build();
	}
	
	/**
	 * /logoutの後に呼ばれる /oauth2/v1/userlogoutをcallしたときにセットしたstateはここで返ってくる
	 * 待ち合わせしているスレッドにstate service経由でイベントを通知してあげる
	 * */
	@GET
	@Path("/post_logout")
	//@Produces(MediaType.TEXT_PLAIN)
	public Response oauthPostLogout(@QueryParam(value="state") String state, @Context UriInfo uriInfo) {
		logger.info("/post_logout called, state: " + state);
		if(null != state && 0 != state.length()){
			try{
				StateService.put(state, "FINISH");
			}catch(Exception e){
				logger.error("Coundn't update remote state: " + e.getMessage(), e);
			}
		}
		return Response.ok().entity("ログアウトしました..").build();
	}
	
	
    /**
     * SPAからキックされるログアウトの継続処理
     * /post_logoutがコールされる（クラスタリングされている場合別サーバーの可能性あり）まで待ち合せる
     * */
	@GET
	@Path("/logout_result")
	//@Produces(MediaType.APPLICATION_JSON)
	public void oauthLogoutResult(@QueryParam(value="state") String state) throws Exception{
		logger.info("/logout_result called, state: " + state);
		
		try{
			// とりあえず30秒待ち合わせ
			String code = null;;
			for(int i = 0 ; i < 30 ; i++){
				try{
					Thread.sleep(1000);
				}catch(InterruptedException ognore){}
				code = StateService.get(state);
				if(null != code){
					break;
				}
			}
			if(null == code){
				throw new Exception("timeout.");
			}
		}finally{
			StateService.delete(state);
		}
		
	}
			
	//////////////////////////////////////////////////////////////////////////

	/**
	 * おまけのパスワードチェンジャー
	 * @throws GeneralSecurityException 
	 * */
	@GET
	@Path("/util/chgpw")
	@Produces(MediaType.TEXT_PLAIN)
	public String changePassword(@QueryParam(value="id") String id, @QueryParam(value="pw") String pw) throws Exception {
		Client c = ForJaxRsClient.getLooseSslClient();
		WebTarget target = c.target(idcsURL).path(API_TOKEN);
/*
Client Credentials Grant Type
https://docs.oracle.com/cloud/latest/identity-cloud/IDCSA/op-oauth2-v1-token-post.html#GUID-51E5C29A-6B7E-487A-8832-5D709410C16A__ClientCredentialsGrantTypeRequestAn-29E1DB6A
Use the urn:opc:idm:__myscopes__ scope when you need to obtain an access token that contains all of the allowed scopes.
*/

		Builder builder = target.request()
							.accept("*/*")
							.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(clientID.concat(":").concat(clientSecret).getBytes()));
		if(null != idcsHost){
			builder.header("Host", idcsHost);
		}
		Response response = builder.post(Entity.entity(
				"grant_type=client_credentials&scope=urn:opc:idm:__myscopes__",
				"application/x-www-form-urlencoded;charset=UTF-8"));

		if(2 != response.getStatus()/100){
			throw new Exception("(" + response.getStatus() + ")" + response.readEntity(String.class));
		}
		AccessTokenInfo token = response.readEntity(AccessTokenInfo.class);
		logger.info("access_token: " + token.access_token);

		target = c.target(idcsURL).path(API_PWAUTH).path(id);
		builder = target.request()
							.accept("*/*")
							.header("Authorization", "Bearer " + token.access_token);
		if(null != idcsHost){
			builder.header("Host", idcsHost);
		}
/*
https://docs.oracle.com/cloud/latest/identity-cloud/IDCSA/api-Identity-User%20Password%20Changer.html
	/admin/v1/UserPasswordChanger/{user id}
		{
			"password": "hogehoge", 
			"schemas": ["urn:ietf:params:scim:schemas:oracle:idcs:UserPasswordChanger"]
		}
*/
    	JsonObjectBuilder json = Json.createObjectBuilder();
    	json.add("password", pw);
    	JsonArrayBuilder jsonArray = Json.createArrayBuilder();
    	jsonArray.add("urn:ietf:params:scim:schemas:oracle:idcs:UserPasswordChanger");
    	json.add("schemas", jsonArray);

		response = builder.put(Entity.entity(json.build().toString(), "application/scim+json"));
		if(2 != response.getStatus()/100){
			throw new Exception("(" + response.getStatus() + ")" + response.readEntity(String.class));
		}
		
		return response.readEntity(String.class);
	}
	
	//////////////////////////////////////////////////////////////////////////
	public void checkToken(AccessTokenInfo token){
		try{
			SignedJWT jwtAccess = SignedJWT.parse(token.access_token);
			JWTClaimsSet claimSet = jwtAccess.getJWTClaimsSet();
			Map<String, Object> claims = claimSet.getClaims();
			for(String s : claims.keySet()){
				logger.info("[Access Token] " + s + ": " + claims.get(s).toString());
			}
		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}

		try{
			SignedJWT jwtId = SignedJWT.parse(token.id_token);
			JWTClaimsSet claimSet = jwtId.getJWTClaimsSet();
			Map<String, Object> claims = claimSet.getClaims();
			for(String s : claims.keySet()){
				logger.info("[Id Token] " + s + ": " + claims.get(s).toString());
			}
		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}
	
	
	}
	
	
	//////////////////////////////////////////////////////////////////////////
	
	
	//state serviceに任せないとクラスタリングで破綻する
    public static class StateService{
    
    	private static final String mapName = "__IDCS_OAUTH_STATE__";
    	
    	public static void put(String state, String code) throws GeneralSecurityException{
            Client c = ForJaxRsClient.getLooseSslClient();
            try{
        		WebTarget target = c.target(AuthRestService.stateServiceURL);
                
                Response response = target.path("state").path(mapName).path(state)
                		.request()
                		.header(AuthRestService.X_CLIENT_NAME, AuthRestService.hostName)
                		.put(Entity.entity(code, MediaType.TEXT_PLAIN));
            
                logger.info("POST: " + mapName + "(" + response.getStatus() + ") - state=" + state + ", code=" + code);
            }finally{
            	if(null != c)	c.close();
            }

    	}

    	public static String get(String state) throws GeneralSecurityException{
            Client c = ForJaxRsClient.getLooseSslClient();
            try{
        		WebTarget target = c.target(AuthRestService.stateServiceURL);
                
                Response response = target.path("state").path(mapName).path(state)
                		.request()
                		.header(AuthRestService.X_CLIENT_NAME, AuthRestService.hostName)
                		.get();

                int status = response.getStatus();
                logger.info("GET: " + status  + " - state=" + state);
                if(status / 100 != 2){
                	return null; // まだ登録されていないケースはこれ
                }
                KeyValue keyValue = response.readEntity(KeyValue.class);
                if(null == keyValue){
                	return null;
                }
                return keyValue.getValue();
            }finally{
            	if(null != c)	c.close();
            }
    	}

    	public static void delete(String state) throws GeneralSecurityException{
            Client c = ForJaxRsClient.getLooseSslClient();
            try{
        		WebTarget target = c.target(AuthRestService.stateServiceURL);
                
                Response response = target.path("state").path(mapName).path(state)
                		.request()
                		.header(AuthRestService.X_CLIENT_NAME, AuthRestService.hostName)
                		.delete();
                if(response.getStatus() / 100 != 2){
                	logger.error("Couldn't delete state: (" + response.getStatus() + ") " + response.getStatusInfo().getReasonPhrase());
                }
            }finally{
            	if(null != c)	c.close();
            }
    	}

    	
    }
	
	
}


