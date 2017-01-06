package com.example.auth;

import java.util.Map;

public interface Authenticator {
	
	public static final String IDENTITY = "__IDENTITY__";
	public static final String USERNAME = "__USERNAME__";
	
	public Map<String, String> authenticate(String username, String password) throws Exception;
}
