package com.example;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("/")
public class MyResource {

    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     */
    @GET
    @Path("/config")
    @Produces(MediaType.APPLICATION_JSON)
    public String config() {
    	System.out.println("<<<config>>>");
    	
    	String auth_type = System.getenv("AUTH_TYPE");
    	if(null == auth_type) auth_type = "SCIM";
    	
    	JsonObjectBuilder json = Json.createObjectBuilder();

    	json.add("auth_type", auth_type);
    	
    	return json.build().toString();
    }
}
