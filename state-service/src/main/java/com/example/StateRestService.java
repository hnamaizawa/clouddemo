package com.example;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
@Path("/state")
public class StateRestService {

	public static final String X_CLIENT_NAME = "X-CLIENT-NAME";

	private Logger logger = LoggerFactory.getLogger(getClass());

	private static HashMap<String, HashMap<String, String>> maps = new HashMap<String, HashMap<String,String>>();

	public static class KeyValue{
		
		private String key;
		private String value;
		
		public KeyValue(){}

		public KeyValue(String key, String value){
			this.key = key;
			this.value = value;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
		
	}
	

	@POST @Path("/{map}")
	@Consumes(MediaType.APPLICATION_JSON)
	public void setKeyValue(@PathParam("map") String mapName, KeyValue[] items, @Context HttpHeaders headers){
		String clientName = headers.getHeaderString(X_CLIENT_NAME);
		logger.info("[" + (null == clientName ? "unknown" : clientName) + "] " + "POST - map: " + mapName);

		synchronized(maps){
			HashMap<String, String> map = maps.get(mapName);
			if(null == map){
				map = new HashMap<String, String>();
				maps.put(mapName, map);
			}
			for(KeyValue item : items){
				map.put(item.getKey(), item.getValue());
			}
		}
	}
	
	
	@PUT @Path("/{map}/{key}")
	public void setValue(@PathParam("map") String mapName, @PathParam("key") String key, String value, @Context HttpHeaders headers){
		String clientName = headers.getHeaderString(X_CLIENT_NAME);
		logger.info("[" + (null == clientName ? "unknown" : clientName) + "] " + "PUT - map: " + mapName + ", key: " + key);

		synchronized(maps){
			HashMap<String, String> map = maps.get(mapName);
			if(null == map){
				map = new HashMap<String, String>();
				maps.put(mapName, map);
			}
			map.put(key, value);
		}
	}
	
	
	@GET @Path("/{map}/{key}")
	@Produces(MediaType.APPLICATION_JSON)
	public KeyValue getValue(@PathParam("map") String mapName, @PathParam("key") String key, @Context HttpHeaders headers) throws Exception{
		String clientName = headers.getHeaderString(X_CLIENT_NAME);
		logger.info("[" + (null == clientName ? "unknown" : clientName) + "] " + "GET - map: " + mapName + ", key: " + key);

		HashMap<String, String> map = maps.get(mapName);
		if(null == map){
			throw new Exception("No map found: " + mapName);
		}
		return new KeyValue(key, map.get(key));
	}
	
	@GET @Path("/{map}")
	@Produces(MediaType.APPLICATION_JSON)
	public KeyValue[] getValues(@PathParam("map") String mapName, @Context HttpHeaders headers) throws Exception{
		String clientName = headers.getHeaderString(X_CLIENT_NAME);
		logger.info("[" + (null == clientName ? "unknown" : clientName) + "] " + "GET - map: " + mapName);

		HashMap<String, String> map = maps.get(mapName);
		if(null == map){
			throw new Exception("No map found: " + mapName);
		}
		List<KeyValue> list = new ArrayList<KeyValue>();
		for(String key : map.keySet()){
			list.add(new KeyValue(key, map.get(key)));
		}
		return list.toArray(new KeyValue[list.size()]);
	}

	@DELETE @Path("/{map}/{key}")
	public void deleteValue(@PathParam("map") String mapName, @PathParam("key") String key, @Context HttpHeaders headers) throws Exception{
		String clientName = headers.getHeaderString(X_CLIENT_NAME);
		logger.info("[" + (null == clientName ? "unknown" : clientName) + "] " + "DELETE - map: " + mapName + ", key: " + key);
		synchronized(maps){
			HashMap<String, String> map = maps.get(mapName);
			if(null == map){
				throw new Exception("No map found: " + mapName);
			}
			synchronized(map){
				map.remove(key);
			}
		}
	}
	
	@DELETE @Path("/{map}")
	public KeyValue[] deleteValue(@PathParam("map") String mapName, @Context HttpHeaders headers) throws Exception{
		String clientName = headers.getHeaderString(X_CLIENT_NAME);
		logger.info("[" + (null == clientName ? "unknown" : clientName) + "] " + "DELETE - map: " + mapName);

		HashMap<String, String> map;
		synchronized(maps){
			map = maps.get(mapName);
			if(null == map){
				return null;
			}
			maps.remove(mapName);
		}
		List<KeyValue> list = new ArrayList<KeyValue>();
		for(String key : map.keySet()){
			list.add(new KeyValue(key, map.get(key)));
		}
		return list.toArray(new KeyValue[list.size()]);
	}

	
	
	@GET @Path("/dump")
	@Produces(MediaType.TEXT_PLAIN)
	public String dump(){
		Date date = new Date();
		StringBuffer buf = new StringBuffer();
		buf.append(date.toString());
		buf.append("\r\n");
		buf.append(">> START\r\n");
		for(String mapName : maps.keySet()){
			buf.append("Map: " + mapName + "\r\n");
			HashMap<String, String> map = maps.get(mapName);
			for(String key : map.keySet()){
				String value = map.get(key);
				buf.append("{key: " + key + ", value: " + value + "}\r\n");
			}
		}
		buf.append("<< END\r\n");
		return buf.toString();
	}
	

}



