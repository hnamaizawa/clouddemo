package com.example;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

//import org.glassfish.jersey.server.ResourceConfig;

import com.example.auth.AuthFilter;
import com.example.auth.AuthRestService;
import com.example.auth.IDCSOAuthService;
import com.example.filters.CORSFilter;

@ApplicationPath("/")
public class WebapiConfig extends /*ResourceConfig*/ Application{
/*
	public WebapiConfig() {
//		packages(this.getClass().getPackage().getName());
//		packages("com.github.kamegu.first");
		packages("com.example");
		packages("com.example.auth");
        register(CORSFilter.class);
        register(AuthFilter.class);	
        register(GlobalExceptionMapper.class);	
    }
*/

	@Override
	public Set<Class<?>> getClasses() {
        Set<Class<?>> s = new HashSet<Class<?>>();
        s.add(MyResource.class);
        s.add(TestRestService.class);
        s.add(DatabaseRestService.class);
        s.add(DatabaseRestServiceSample.class);
        s.add(AuthRestService.class);
        s.add(IDCSOAuthService.class);
        s.add(CORSFilter.class);
        s.add(AuthFilter.class);
        s.add(GlobalExceptionMapper.class);
        return s;
    }

}
