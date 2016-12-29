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
public class DummyAuthenticator implements Authenticator{
	private Logger logger = LoggerFactory.getLogger(getClass());

	
	public DummyAuthenticator(){
		
	}
	
	/**
	 * なんちゃってユーザー認証を行う
	 * 
	 * 
	 * */
	@Override
	public Map<String, String> authenticate(String username, String password) throws Exception{

		if(username.equalsIgnoreCase("oracle") && password.equalsIgnoreCase("welcome1")){
			Map<String, String> identity = new Hashtable<String, String>();
			identity.put(USERNAME, "oracle");
			identity.put(IDENTITY, "cn=oracle");
			return identity;
		}
		throw new Exception("No entry found: " + username);
	}

	
	
}


