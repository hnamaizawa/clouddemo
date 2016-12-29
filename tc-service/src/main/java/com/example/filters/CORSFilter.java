package com.example.filters;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;



@CORS
public class CORSFilter implements ContainerResponseFilter {

	private String serverName;
	private static final String X_SERVER_NAME = "X-SERVER-NAME";
	private static final String HOSTNAME = "HOSTNAME";

	public CORSFilter(){
		serverName = System.getenv(HOSTNAME);
		if(null == serverName) serverName = "unknown";
	}

	
	
	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {

		//System.out.println("CORS Filter...");
		
		MultivaluedMap<String, Object> headers = responseContext.getHeaders();
		
		headers.add("Access-Control-Allow-Origin", "*");
		headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, PATCH, DELETE");
		headers.add("Access-Control-Allow-Headers", "X-Requested-With,content-type");
		headers.add("Access-Control-Allow-Credentials", true);
		
		headers.add(X_SERVER_NAME, serverName);

	}
}