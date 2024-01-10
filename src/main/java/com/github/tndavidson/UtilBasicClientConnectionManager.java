package com.github.tndavidson;



import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.SSLContexts;

public class UtilBasicClientConnectionManager {

	 public static HttpClient buildHttpClient(HttpClientConfig config) {
		 
		  final RequestConfig requestConfig = RequestConfig.custom()
	                .setConnectionRequestTimeout(config.getConnectionRequestTimeout(), TimeUnit.MILLISECONDS)
	                .setConnectTimeout(config.getConnectTimeout(), TimeUnit.MILLISECONDS)
	                .setResponseTimeout(config.getSocketTimeout(), TimeUnit.MILLISECONDS)
	                .build();

	        HttpClientBuilder httpClientBuilder = HttpClients.custom()
	                .setDefaultRequestConfig(requestConfig);
	        SSLContext sslContext = null;
	        if (StringUtils.isNotEmpty(config.getKeyStore())) {
	            sslContext = sslContext(config);
	        }
	        SSLConnectionSocketFactory sslsf = 
	                new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
	        Registry<ConnectionSocketFactory> socketFactoryRegistry = 
	                RegistryBuilder.<ConnectionSocketFactory> create()
	                .register("https", sslsf)
	                .register("http", new PlainConnectionSocketFactory())
	                .build();

	        
	        if (sslContext != null) {
	        	new BasicHttpClientConnectionManager(socketFactoryRegistry);
	        }     
	       
	        BasicHttpClientConnectionManager httpClientManager=new BasicHttpClientConnectionManager(socketFactoryRegistry);
	        httpClientBuilder.setConnectionManager(httpClientManager);
	        
	   return httpClientBuilder.build();
}
	 public static SSLContext sslContext(final HttpClientConfig config) {
	        final SSLContextBuilder sslContextBuilder = SSLContexts.custom().setProtocol(config.getProtocol());

	        try {
	            sslContextBuilder
	                    .loadKeyMaterial(new File(FilenameUtils.normalize(config.getKeyStore())),
	                            config.getKeyStorePassword().toCharArray(),
	                            config.getKeyPassword().toCharArray(),
	                            (aliases, socket) -> config.getKeyAlias());

	            if (config.getTrustStore() != null) {
	                sslContextBuilder.loadTrustMaterial(new File(FilenameUtils.normalize(config.getTrustStore())),
	                        config.getTrustStorePassword().toCharArray());
	            }
	            return sslContextBuilder.build();
	        } catch (GeneralSecurityException | IOException e) {
	            throw new IllegalArgumentException(e);
	        }
	    }
}