package com.example;

import java.net.UnknownHostException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.auth.Auth;

/**
 * Root resource (exposed at "myresource" path) 
 */
@Path("/test")
public class TestRestService {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	/**
	 * Method handling HTTP GET requests. The returned object will be sent to
	 * the client as "text/plain" media type.
	 *
	 * @return String that will be returned as a text/plain response.
	 */
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String root() {
		return "Got it!";
	}


	@GET
	@Path("/diag")
	@Produces(MediaType.TEXT_PLAIN)
	public String diag() throws UnknownHostException {
		DateFormat format = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.FULL, Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("JST"));

		return format.format(new Date());
	}

	@GET
	@Path("/hello")
	@Produces(MediaType.TEXT_PLAIN)
	public String hello() {
		return "Hello World!";
	}

	@GET
	@Path("/echo")
	//@Produces(MediaType.TEXT_PLAIN)
	public /*String*/ Response echo(@QueryParam("message") String message) {
		return Response.ok(message, new  MediaType("text", "plain; charset=UTF-8")).build();
		//return message;
	}

	@GET
	@Path("/echo/{message}")
	//@Produces(MediaType.TEXT_PLAIN)
	public /*String*/ Response echo2(@PathParam("message") String message) {
		return Response.ok(message, new  MediaType("text", "plain; charset=UTF-8")).build();
		//return message;
	}

}


