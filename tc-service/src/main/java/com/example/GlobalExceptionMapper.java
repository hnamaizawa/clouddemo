package com.example;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RSの例外を一手に引き受ける
 * */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

	private static Logger logger = LoggerFactory.getLogger(GlobalExceptionMapper.class);

	
	@Override
	public Response toResponse(Throwable e) {
		logger.info("Caught Throwable: " + e.getMessage(), e);

		return Response
				.status(Status.INTERNAL_SERVER_ERROR)
				// ExceptionMapperで拾っておくと後続のFilterに処理が渡るので、これは不要
				//.header(X_SERVER_NAME, serverName)
				.entity(e.getMessage())
				.build();
	}

}
