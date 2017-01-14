package com.example;

import org.glassfish.grizzly.http.server.HttpServer;

/**
 * 
 */
public class SetupTestServer {

	// Base URI the Grizzly HTTP server will listen on
    public final static String BASE_URI = "http://localhost:8088/hoge";

    private static HttpServer server;
	
	public static void setup(){
		if(null != server){
			return;
		}
		
    	// start the server
        server = Main.startServer(BASE_URI);
        /* Gradleだとここでエラーになるようなので、shutdownhookは使わない
        Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run() {
				server.shutdownNow();
				System.out.println("server shutdown.");
			}
        });
        */
		
	}
	
	public static void shutdown(){
		if(null != server){
			server.shutdown();
			System.out.println("server shutdown.");
		}
	}

	
}
