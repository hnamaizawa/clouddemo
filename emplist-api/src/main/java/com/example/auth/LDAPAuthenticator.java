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
 */
public class LDAPAuthenticator implements Authenticator{
	private Logger logger = LoggerFactory.getLogger(getClass());

	private static final String LDAP_SERVER = "ldap://ldap.oracle.com:636";
	private static final String TLS_PROTOCOLS = "jdk.tls.client.protocols";
	
	
	public LDAPAuthenticator(){
		
	}
	
	/**
	 * LDAPをルックアップしてユーザー認証を行う
	 * 
	 * 
	 * */
	@Override
	public Map<String, String> authenticate(String username, String password) throws Exception{

		// 匿名でアクセスして、mailからDNを割り出して、認証できるかどうかを確認する
		
		javax.naming.ldap.LdapContext ctx = null;
		
		try{
			if(null == System.getProperty(TLS_PROTOCOLS)){
				System.setProperty(TLS_PROTOCOLS,"TLSv1"); // ldap.oracle.comのアクセスに必要
			}
			logger.debug(TLS_PROTOCOLS + ": " + System.getProperty(TLS_PROTOCOLS));
			
			Hashtable<String, Object> env = new Hashtable<String, Object>();
			env.put(javax.naming.ldap.LdapContext.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			env.put(javax.naming.ldap.LdapContext.PROVIDER_URL, LDAP_SERVER);
			env.put(javax.naming.ldap.LdapContext.SECURITY_AUTHENTICATION, "simple");
			//env.put(javax.naming.ldap.LdapContext.SECURITY_PRINCIPAL, username);
			//env.put(javax.naming.ldap.LdapContext.SECURITY_CREDENTIALS, password);
			env.put(javax.naming.ldap.LdapContext.SECURITY_PROTOCOL, "ssl");
			//env.put("com.sun.jndi.ldap.trace.ber", System.err);

			ctx = new InitialLdapContext(env, null);
			
			SearchControls searchControls = new SearchControls();
			searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			NamingEnumeration<SearchResult> searchResults = ctx.search(
					"dc=oracle,dc=com", 
					"(&(mail="+ username + ")(objectclass=person))", 
					searchControls);
		
			if(searchResults.hasMore()){
				SearchResult result = searchResults.next();
				String dn = result.getNameInNamespace();
				//System.out.println(dn);

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

	
	public static void main(String[] args) throws Exception{
		LDAPAuthenticator auth = new LDAPAuthenticator();
		auth.authenticate("tadahisa.kotegawa@oracle.com", "xxx");
		System.out.println("OK");
	}
	

	
}


