package com.example;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.filters.CORSFilter;

import java.net.InetAddress;
import java.net.URI;

/**
 * Main class.
 */
public class Main {

	private static final String DEFAULT_HOST = "localhost";
	private static final String DEFAULT_PORT = "8088";

	private static final String HOSTNAME = "HOSTNAME";
    private static final String PORT = "PORT";
    private static final String API_ROOT = "API_ROOT"; 
    private static final String STATIC_DIR = "STATIC_DIR"; 

    private static String base_uri;
	
	private static Logger logger = LoggerFactory.getLogger(Main.class);

    
    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
     * @return Grizzly HTTP server.
     */
    public static HttpServer startServer(String uri) {
    	base_uri = uri;

    	//SSLContextConfigurator sslContext = new SSLContextConfigurator();    	
        //sslContext.setKeyStoreFile(KEYSTORE_SERVER_FILE); // contains server keypair
        //sslContext.setKeyStorePass(KEYSTORE_SERVER_PWD);
        //sslContext.setTrustStoreFile(TRUSTORE_SERVER_FILE); // contains client certificate
        //sslContext.setTrustStorePass(TRUSTORE_SERVER_PWD);

    	// create a resource config that scans for JAX-RS resources and providers
        // in com.example package
        final ResourceConfig rc = new ResourceConfig().packages("com.example");
        rc.register(CORSFilter.class);
        //rc.registerClasses(RootResource.class, SecurityFilter.class, AuthenticationExceptionMapper.class);
        
        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(
        		URI.create(base_uri), 
        		rc
        		//,true
        		//,new SSLEngineConfigurator(sslContext).setClientMode(false).setNeedClientAuth(true)
        		);
//        if(null != static_dir && 0 != static_dir.length()){
//            server.getServerConfiguration().addHttpHandler(new StaticHttpHandler(static_dir), "/");
//        }
        return server;
    }

    /**
     * Main method.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args){

    	
    	try{
        	// これは環境変数
    		String hostname = System.getenv(HOSTNAME);
        	if(null == hostname) hostname = DEFAULT_HOST;
        	String port = System.getenv(PORT);
        	if(null == port) port = DEFAULT_PORT;
        	
        	// こっちはシステムプロパティ
        	String api_root = System.getProperty(API_ROOT, "api");
        	//String static_dir = System.getProperty(STATIC_DIR, "./static");
        			
        	logger.info("HOSTNAME: " + hostname);
        	logger.info("PORT: " + port);
        	logger.info("CONTEXT_ROOT: " + api_root);
        	
        	String base_uri = "http://" + hostname + ":" + port + "/" + api_root; // + "/";
           	logger.info("BASE_URI: " + base_uri);
                   	
    		final HttpServer server = startServer(base_uri);

    		Runtime.getRuntime().addShutdownHook(new Thread(){
    			@Override
    			public void run() {
    				server.shutdownNow();
    				logger.info("Server shutdown.");
    			}
            });
            
            
    	}catch(Exception e){
    		logger.error(e.getMessage(), e);
    		System.exit(1);
    	}
    	
    }

    public static String getBaseUri(){
    	return base_uri;
    }
    
}




