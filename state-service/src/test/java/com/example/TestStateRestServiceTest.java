package com.example;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.example.StateRestService.KeyValue;


import static org.junit.Assert.assertEquals;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestStateRestServiceTest {

    private static WebTarget target;
    
	public static final String X_CLIENT_NAME = "X-CLIENT-NAME";
	private static String hostName;
	static{
		try{
			hostName = InetAddress.getLocalHost().getHostName();
		}catch(Exception e){}
	}


    @BeforeClass
    public static void setUp() throws Exception {

    	SetupTestServer.setup();
    	
        // create the client
        Client c = ClientBuilder.newClient();

        // uncomment the following line if you want to enable
        // support for JSON in the client (you also have to uncomment
        // dependency on jersey-media-json module in pom.xml and Main.startServer())
        // --
        // c.configuration().enable(new org.glassfish.jersey.media.json.JsonJaxbFeature());

        target = c.target(SetupTestServer.BASE_URI);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        //server.shutdown();
    }

    /**
     * Test to see that the message "Got it!" is sent in the response.
     */
    @Test
    public void test01_Post() {
		List<StateRestService.KeyValue> list = new ArrayList<StateRestService.KeyValue>();
		list.add(new StateRestService.KeyValue("aaa", "bbb"));
		list.add(new StateRestService.KeyValue("xxx", "yyy"));
		
        Client c = ClientBuilder.newClient();
        WebTarget target = c.target(SetupTestServer.BASE_URI);
        
        Response response = target.path("state/map1")
        		.request()
        		.header(X_CLIENT_NAME, hostName)
        		.post(Entity.entity(list.toArray(
        				new StateRestService.KeyValue[list.size()]), MediaType.APPLICATION_JSON_TYPE));
        System.out.println("POST status: " + response.getStatus());

        response = target.path("state/map1")
        		.request(MediaType.APPLICATION_JSON_TYPE)
        		.header(X_CLIENT_NAME, hostName)
        		.get();
        		//.get(StateRestService.KeyValue[].class);
        System.out.println("GET status: " + response.getStatus());
        StateRestService.KeyValue[] items = response.readEntity(StateRestService.KeyValue[].class);
        HashMap<String, String> map = new HashMap<String, String>();
        for(StateRestService.KeyValue item : items){
        	map.put(item.getKey(), item.getValue());
        }
        assertEquals(map.get("aaa"), "bbb");
        assertEquals(map.get("xxx"), "yyy");
    }

    @Test
    public void test02_Put() {
		
        Client c = ClientBuilder.newClient();
        WebTarget target = c.target(SetupTestServer.BASE_URI);
        
        Response response = target.path("state/map1/foo")
        		.request()
        		.header(X_CLIENT_NAME, hostName)
        		.put(Entity.entity("bar", MediaType.TEXT_PLAIN));
        System.out.println("PUT status: " + response.getStatus());

        response = target.path("state/map1/foo")
        		.request(MediaType.APPLICATION_JSON_TYPE)
        		.header(X_CLIENT_NAME, hostName)
        		.get();
        System.out.println("GET status: " + response.getStatus());
        KeyValue item = response.readEntity(KeyValue.class);
        assertEquals(item.getKey(), "foo");
        assertEquals(item.getValue(), "bar");

        response = target.path("state/map1/boo")
        		.request(MediaType.APPLICATION_JSON_TYPE)
        		.header(X_CLIENT_NAME, hostName)
        		.get();
        System.out.println("GET status: " + response.getStatus());
        item = response.readEntity(KeyValue.class);
        assertEquals(item.getKey(), "boo");
        Assert.assertNull(item.getValue());
    }

    @Test
    public void test03_Delete() {
		
        dump();
        Client c = ClientBuilder.newClient();
        WebTarget target = c.target(SetupTestServer.BASE_URI);
        
        Response response = target.path("state/mapX")
        		.request(MediaType.APPLICATION_JSON_TYPE)
        		.header(X_CLIENT_NAME, hostName)
        		.delete();
        System.out.println("DELETE status: " + response.getStatus());

        StateRestService.KeyValue[] items = response.readEntity(StateRestService.KeyValue[].class);
        Assert.assertNull(items);

        dump();
        response = target.path("state/map1")
        		.request(MediaType.APPLICATION_JSON_TYPE)
        		.header(X_CLIENT_NAME, hostName)
        		.delete();
        System.out.println("DELETE status: " + response.getStatus());
        items = response.readEntity(StateRestService.KeyValue[].class);
        Assert.assertNotNull(items);

        dump();
        HashMap<String, String> map = new HashMap<String, String>();
        for(StateRestService.KeyValue item : items){
        	map.put(item.getKey(), item.getValue());
        }
        assertEquals(map.get("aaa"), "bbb");
        assertEquals(map.get("xxx"), "yyy");
        assertEquals(map.get("foo"), "bar");
    }

    public void dump(){
        Client c = ClientBuilder.newClient();
        WebTarget target = c.target(SetupTestServer.BASE_URI);
        
        Response response = target.path("state/dump")
        		.request(MediaType.TEXT_PLAIN)
        		.header(X_CLIENT_NAME, "x")
        		.get();
        System.out.println(response.readEntity(String.class));
    }
    
    
}
