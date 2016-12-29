package com.example;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;

import com.example.auth.AuthFilter;
import com.example.filters.CORSFilter;

@ApplicationPath("")
public class WebapiConfig extends ResourceConfig {
	public WebapiConfig() {
//		packages(this.getClass().getPackage().getName());
//		packages("com.github.kamegu.first");
		packages("com.example");
		packages("com.example.auth");
        register(CORSFilter.class);
        register(AuthFilter.class);	
        register(GlobalExceptionMapper.class);	
    }
}
