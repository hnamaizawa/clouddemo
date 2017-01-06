package com.example.auth;


import java.util.Hashtable;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Authentication
 * 組み込みLDAPで認証してみる
 * 
 * WebLogicの組み込みLDAPはanonymousアクセスは許可しません
 * 
 * rest.auth.class=com.example.auth.WLSLDAPAuthenticator
 * LDAP_URL=ldap://1.2.3.4:80
 * LDAP_DOMAIN=LDAP_domain
 * 
 */
public class WLSLDAPAuthenticator implements Authenticator{
	private Logger logger = LoggerFactory.getLogger(getClass());

	private static final String LDAP_URL = "LDAP_URL";
	private static final String LDAP_DOMAIN = "LDAP_DOMAIN";
	private static final String LDAP_ADMIN_PASSWORD = "LDAP_ADMIN_PASSWORD";
	private static final String LDAP_ADMIN_PW_DEFAULT = "welcome1";
	//private static final String TLS_PROTOCOLS = "jdk.tls.client.protocols";
	
	
	private String ldapURL;
	private String ldapDomain;
	private String ldapAdminPassword;
	
	public WLSLDAPAuthenticator(){
		ldapURL = System.getenv(LDAP_URL);
		ldapDomain = System.getenv(LDAP_DOMAIN);
		ldapAdminPassword = System.getenv(LDAP_ADMIN_PASSWORD);
		if(null == ldapAdminPassword){
			ldapAdminPassword = LDAP_ADMIN_PW_DEFAULT;
		}
	}
	
	/**
	 * LDAPをルックアップしてユーザー認証を行う
	 * 
	 * 
	 * */
	@Override
	public Map<String, String> authenticate(String username, String password) throws Exception{

		javax.naming.ldap.LdapContext ctx = null;
		
		try{
			
			Hashtable<String, Object> env = new Hashtable<String, Object>();
			env.put(javax.naming.ldap.LdapContext.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			env.put(javax.naming.ldap.LdapContext.PROVIDER_URL, ldapURL);
			env.put(javax.naming.ldap.LdapContext.SECURITY_AUTHENTICATION, "simple");
			env.put(javax.naming.ldap.LdapContext.SECURITY_PRINCIPAL, "cn=Admin");
			env.put(javax.naming.ldap.LdapContext.SECURITY_CREDENTIALS, ldapAdminPassword);
			if(ldapURL.toUpperCase().startsWith("LDAPS")){
				env.put(javax.naming.ldap.LdapContext.SECURITY_PROTOCOL, "ssl");
			}
			//env.put("com.sun.jndi.ldap.trace.ber", System.err);

			ctx = new InitialLdapContext(env, null);
			
			SearchControls searchControls = new SearchControls();
			searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			NamingEnumeration<SearchResult> searchResults = ctx.search(
					"dc=" + ldapDomain, 
					"(&(cn="+ username + ")(objectclass=person))", 
					searchControls);
		
			if(searchResults.hasMore()){
				SearchResult result = searchResults.next();
				String dn = result.getNameInNamespace();
				logger.info("DN: " + dn);

				// DNを使って再認証
				ctx.addToEnvironment(javax.naming.ldap.LdapContext.SECURITY_PRINCIPAL, dn);
				ctx.addToEnvironment(javax.naming.ldap.LdapContext.SECURITY_CREDENTIALS, password);
				ctx.reconnect(null); // 認証失敗するとここでjavax.naming.AuthenticationException
				Map<String, String> identity = new Hashtable<String, String>();
				identity.put(USERNAME, username);
				identity.put(IDENTITY, dn);
				return identity;
			}
			throw new NamingException("No entry found: " + username);
		}finally{
			if(null != ctx) ctx.close();
		}
		
	}

	


	public String getLdapURL() {
		return ldapURL;
	}

	public void setLdapURL(String ldapURL) {
		this.ldapURL = ldapURL;
	}

	public String getLdapDomain() {
		return ldapDomain;
	}

	public void setLdapDomain(String ldapDomain) {
		this.ldapDomain = ldapDomain;
	}

	public String getLdapAdminPassword() {
		return ldapAdminPassword;
	}

	public void setLdapAdminPassword(String ldapAdminPassword) {
		this.ldapAdminPassword = ldapAdminPassword;
	}
	
	
	public static void main(String[] args) throws Exception{
		WLSLDAPAuthenticator auth = new WLSLDAPAuthenticator();
		auth.setLdapDomain("LDAP_domain");
		auth.setLdapURL("ldap://140.86.1.104:80");
		auth.authenticate("oracle", "welcome1");
		System.out.println("OK");
	}

	
}


