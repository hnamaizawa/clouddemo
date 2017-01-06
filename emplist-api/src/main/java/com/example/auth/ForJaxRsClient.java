package com.example.auth;

import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

public class ForJaxRsClient {
	
	//static{
	//	// CXFのJAX-RS実装を使わせる
	//	System.setProperty("javax.ws.rs.client.ClientBuilder", "org.apache.cxf.jaxrs.client.spec.ClientBuilderImpl");
	//}
	
	/**
	 * オレオレ証明書を突破するClient
	 */
	public static Client getLooseSslClient() throws KeyManagementException, NoSuchAlgorithmException {
		Client client =  ClientBuilder.newBuilder().sslContext(getSSLContext()).hostnameVerifier(new TrustAllHostNameVerifier())
				.build();
		//System.out.println("JAX-RS Client: " + client.getClass().getName());
		return client;
	}

	public static SSLContext getSSLContext() throws NoSuchAlgorithmException, KeyManagementException {
		SSLContext ctx = SSLContext.getInstance(/* "SSL" */"TLS");
		ctx.init(null, certs, new SecureRandom());
		return ctx;
	}

	static TrustManager[] certs = new TrustManager[] { new X509TrustManager() {
		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		}
	} };

	public static class TrustAllHostNameVerifier implements HostnameVerifier {

		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	}

}