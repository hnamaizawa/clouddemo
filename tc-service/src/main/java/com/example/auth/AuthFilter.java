package com.example.auth;

import java.io.IOException;
import java.security.Principal;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.auth.AuthRestService.Session;

@Auth
public class AuthFilter implements ContainerRequestFilter {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Inject
	javax.inject.Provider<UriInfo> uriInfo;

	@Override
	public void filter(ContainerRequestContext context) throws IOException {
		
		try {
			String token = context.getHeaderString(AuthRestService.X_AUTH_TOKEN);
			if (null == token || 0 == token.length()) {
				throw new Exception("No X_Auth_Token header.");
			}
			Session session = AuthRestService.validateSession(token);
			context.setProperty(AuthRestService.PROP_AUTH_SESSION, session);
			context.setSecurityContext(new CustomSecurityContext(session));

		} catch (Exception e) {
			logger.debug(e.getMessage());
			String mess = e.getMessage();
			if(null == mess || 0 == mess.length()){
				mess = e.getClass().getSimpleName();
			}
			// 残念ながらTomcatはこのentityではなくTomcatのページを返してしまう...
			context.abortWith(Response.status(Status.UNAUTHORIZED).entity(e.getMessage()).build());
		}
	}

	public class CustomSecurityContext implements SecurityContext {
		private Session session;
		private Principal principal;

		public CustomSecurityContext(final Session session) {
			this.session = session;
			this.principal = new Principal() {
				public String getName() {
					return session.username;
				}
			};
		}

		public Principal getUserPrincipal() {
			return this.principal;
		}

		public boolean isUserInRole(String role) {
			//return (role.equals(user.role));
			return false;
		}

		public boolean isSecure() {
			return "https".equals(uriInfo.get().getRequestUri().getScheme());
		}

		public String getAuthenticationScheme() {
			return "CustomAuth";
		}
	}

}