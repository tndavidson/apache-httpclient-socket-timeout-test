package com.github.tndavidson;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.HttpComponentsClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import reactor.netty.http.Http11SslContextSpec;
import reactor.netty.http.client.HttpClient;



/**
 * Note that all props/timeouts likely can be set using yml
 * Currently setting explicitly in code.
 */
@Configuration
@ComponentScan(basePackages = {"com.github.tndavidson"})
public class HttpWebClientConfig {
    
    private static final Logger LOG = LoggerFactory.getLogger(HttpWebClientConfig.class);

    private final String fakeCertPath = "c:/temp/";
    
    private String ciUrl = "https://localhost:8443/test-tls-ci";
    private String commPermUrl = "https://localhost:8443/test-tls-commperm";
    private String zombieUrl = "https://localhost:8443/test-tls-zombie";
    
    @Bean
    public WireMockServer getWiremockServer() {
//    	WireMockServer wireMockServer = new WireMockServer(8443);
    	
    	WireMockServer wireMockServer = new WireMockServer(WireMockConfiguration.options()
    			.httpsPort(8443)
    			.httpDisabled(true)
    			.asynchronousResponseThreads(50)
    			.containerThreads(100)
                .keystorePath(fakeCertPath + "server-keystore.jks")
                .keystorePassword("secret")
                .keyManagerPassword("secret")
                .trustStorePath(fakeCertPath + "server-truststore.jks")
                .trustStorePassword("secret")
    	);
    	wireMockServer.start();
    	wireMockServer.stubFor(
                get(WireMock.urlMatching("/test-tls-ci")).willReturn(aResponse().withHeader("Content-Type", "application/json").withHeader("Connection", "Keep-Alive")
                		.withBody("{\"addressLine\": \"722 Some Street\", \"city\": \"Alexandria\", \"state\": \"VA\", \"phone\": \"5713217654\", \"email\": \"test@test.com\"}")));
    	wireMockServer.stubFor(
                get(WireMock.urlMatching("/test-tls-commperm")).willReturn(aResponse().withHeader("Content-Type", "application/json").withHeader("Connection", "Keep-Alive")
                        .withBody("{\"channel\": \"text\", \"item\": \"reminder\", \"allowed\": true}")));
    	wireMockServer.stubFor(
                get(WireMock.urlMatching("/test-tls-zombie")).willReturn(aResponse().withHeader("Content-Type", "application/json").withHeader("Connection", "Keep-Alive")
                		.withBody("I am a zombie").withFixedDelay(20000)));
    	
    	
        LOG.debug("Started WireMock Server on 8443");
        return wireMockServer;
    }


//    public void stopWireMock() {
//    	getWiremockServer().stop();
//    }


    private TlsStrategy getApacheTlsConfig() {
        TrustStrategy acceptingTrustStrategy = (certificate, authType) -> true;
        SSLContext sslContext = null;
		try {
			KeyStore keystore = KeyStore.getInstance("JKS");
			keystore.load(new FileInputStream(fakeCertPath + "server-keystore.jks"), "secret".toCharArray());
			sslContext = SSLContexts.custom()
			    .loadTrustMaterial(null, acceptingTrustStrategy)
			    .setKeyStoreType("JKS")
			    .loadKeyMaterial(keystore, "secret".toCharArray())
			    .build();
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException 
				| UnrecoverableKeyException | CertificateException | IOException e) {
			throw new RuntimeException("SSLContext load blew up", e);
		}
        TlsStrategy tlsStrategy = ClientTlsStrategyBuilder.create()
            .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
            .setTlsVersions("TLSv1.3")
            .setSslContext(sslContext)
            .build();
        return tlsStrategy;
    }
    
    private CloseableHttpAsyncClient getApachePooledClientWithTimeouts() {
    	ConnectionConfig config = ConnectionConfig.custom()
  			  .setConnectTimeout(Timeout.ofSeconds(5L))
  			  .setSocketTimeout(Timeout.ofSeconds(8L))
  			  .setTimeToLive(Timeout.ofSeconds(180L))
  			  .setValidateAfterInactivity(Timeout.ofSeconds(60L))
  			  .build();
        PoolingAsyncClientConnectionManager cm = PoolingAsyncClientConnectionManagerBuilder.create()
        	.setDefaultConnectionConfig(config)
            .setTlsStrategy(getApacheTlsConfig())
            .setMaxConnPerRoute(10)
            .setMaxConnTotal(30)
            .build();
    	HttpAsyncClientBuilder clientBuilder = HttpAsyncClients.custom()
    			.setConnectionManager(cm);
    	
    	RequestConfig requestConfig = RequestConfig.custom()
    			.setConnectionKeepAlive(Timeout.ofSeconds(180L))
    			.setResponseTimeout(Timeout.ofSeconds(8L))
    			.setHardCancellationEnabled(true)
    			.build();
    	CloseableHttpAsyncClient client = clientBuilder
    			.setDefaultRequestConfig(requestConfig)
    			.build();
    	return client;
    }
    
    private CloseableHttpAsyncClient getDefaultApacheClient() {
        PoolingAsyncClientConnectionManager cm = PoolingAsyncClientConnectionManagerBuilder.create()
                .setTlsStrategy(getApacheTlsConfig())
                .build();
    	CloseableHttpAsyncClient client = HttpAsyncClientBuilder.create().setConnectionManager(cm).build();
    	return client;
    }
    
    private ClientHttpConnector getPooledApacheAsyncHttpConnector() {
    	return new HttpComponentsClientHttpConnector(getApachePooledClientWithTimeouts());
    }
    
    private ClientHttpConnector getDefaultApacheAsyncHttpConnector() {
    	return new HttpComponentsClientHttpConnector(getDefaultApacheClient());
    }
            
    
    @Bean
    @Qualifier("DefaultCIWebClient") 
    public WebClient getDefaultCIWebClient() {
    	WebClient webClient = WebClient.builder()
			    .baseUrl(ciUrl)
			    .clientConnector(getDefaultApacheAsyncHttpConnector())
				.build();
    	return webClient;
    }
    
    @Bean
    @Qualifier("PooledCIWebClientWithTimeouts") 
    public WebClient getPooledCIWebClientWithTimeouts() {
    	WebClient webClient = WebClient.builder()
			    .baseUrl(ciUrl)
			    .clientConnector(getPooledApacheAsyncHttpConnector())
				.build();
    	return webClient;
    }

    @Bean
    @Qualifier("DefaultCommPermWebClient") 
    public WebClient getDefaultCommPermWebClient() {
    	WebClient webClient = WebClient.builder()
			    .baseUrl(commPermUrl)
			    .clientConnector(getDefaultApacheAsyncHttpConnector())
				.build();
    	return webClient;
    }
    
    @Bean
    @Qualifier("PooledCommPermWebClientWithTimeouts") 
    public WebClient getPooledCommPermWebClientWithTimeouts() {
    	WebClient webClient = WebClient.builder()
			    .baseUrl(commPermUrl)
			    .clientConnector(getPooledApacheAsyncHttpConnector())
				.build();
    	return webClient;
    }
    
    @Bean
    @Qualifier("DefaultZombieWebClient") 
    public WebClient getDefaultZombieWebClient() {
    	WebClient webClient = WebClient.builder()
			    .baseUrl(zombieUrl)
			    .clientConnector(getDefaultApacheAsyncHttpConnector())
				.build();
    	return webClient;
    }
    
    @Bean
    @Qualifier("PooledZombieWebClientWithTimeouts") 
    public WebClient getPooledZombieWebClientWithTimeouts() {
    	WebClient webClient = WebClient.builder()
			    .baseUrl(zombieUrl)
			    .clientConnector(getPooledApacheAsyncHttpConnector())
				.build();
    	return webClient;
    }
    
    
    
    
    
//    /**
//     * Creates a reactor netty HttpClient with timeouts set for http connection
//     * and timeouts for ssl connection (tls v1.3).  Note that closeNotifyReadTimeout set to 1 second
//     * Also creates/uses a connection pool of max 30 connections
//     * @return HttpClient
//     */
//    @Bean
//    public HttpClient getHttpsClientWithTimeouts() {
//    	// HttpClient client = HttpClient.create(ConnectionProvider.create("test-webclient-pool", 15))
//    	HttpClient client = HttpClient.create()
//    			  .wiretap(true)
//    			  // 8 second response timeout (socket timeout)
//    			  .responseTimeout(Duration.ofSeconds(8))
//    			  // 5 second http connection timeout
//    			  .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
//    			  .option(ChannelOption.SO_KEEPALIVE, true);
//    	Http11SslContextSpec http11SslContextSpec = Http11SslContextSpec.forClient().configure(
//    			builder -> {
//    				builder.trustManager(InsecureTrustManagerFactory.INSTANCE);
////    				try {
////						builder.keyManager(KeyManagerFactory.getInstance("TLSv1.3"));
////					} catch (NoSuchAlgorithmException e) {
////					}
//    			});
//        client = client.secure(
//        		spec -> {
//	                spec.sslContext(http11SslContextSpec)
//						    .handshakeTimeout(Duration.ofSeconds(10))
//						    .closeNotifyFlushTimeout(Duration.ofSeconds(1))
//						    // below setting for tls v1.3 will only wait 1 second for close_notify from peer
//						    // after sending a close_notify
//						    .closeNotifyReadTimeout(Duration.ofSeconds(1));
//	        	});
//        LOG.debug("Configured HttpsClient with Timeouts: {}", client.toString());
//    	return client;
//    }
//    
//    
//    @Bean
//    public HttpClient getDefaultHttpsClient() {
//    	HttpClient client = HttpClient.create().wiretap(true);
//    	Http11SslContextSpec http11SslContextSpec = Http11SslContextSpec.forClient().configure(
//    			builder -> {
//    				builder.trustManager(InsecureTrustManagerFactory.INSTANCE);
////    				try {
////						builder.keyManager(KeyManagerFactory.getInstance("TLSv1.3"));
////					} catch (NoSuchAlgorithmException e) {
////					}
//    			});
//        client = client.secure(
//        		spec -> {
//	                spec.sslContext(http11SslContextSpec);
//	        	});
//        LOG.debug("Configured Default HttpsClient: {}", client.toString());
//    	return client;
//    }
}

