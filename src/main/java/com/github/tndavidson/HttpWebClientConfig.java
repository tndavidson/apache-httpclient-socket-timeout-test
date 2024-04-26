package com.github.tndavidson;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
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
    
    private String ciUrl = "https://localhost:8443/test-tls-ci";
    private String commPermUrl = "https://localhost:8443/test-tls-commperm";
    private String zombieUrl = "https://localhost:8443/test-tls-zombie";
    
    private final String fakeCertPath = "c:/temp/";
    
    
    @Bean
    public WireMockServer getWiremockServer() {
    	//WireMockServer wireMockServer = new WireMockServer(8443);
    	
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
                get(WireMock.urlMatching("/test-tls-ci")).willReturn(aResponse().withHeader("Content-Type", "application/json")
                		.withBody("{\"addressLine\": \"722 Some Street\", \"city\": \"Alexandria\", \"state\": \"VA\", \"phone\": \"5713217654\", \"email\": \"test@test.com\"}")));
    	wireMockServer.stubFor(
                get(WireMock.urlMatching("/test-tls-commperm")).willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"channel\": \"text\", \"item\": \"reminder\", \"allowed\": true}")));
    	wireMockServer.stubFor(
                get(WireMock.urlMatching("/test-tls-zombie")).willReturn(aResponse().withHeader("Content-Type", "application/json")
                		.withBody("I am a zombie").withFixedDelay(20000)));
    	
    	
        LOG.debug("Started WireMock Server on 8443");
        return wireMockServer;
    }


//    public void stopWireMock() {
//    	getWiremockServer().stop();
//    }

    /**
     * Creates a reactor netty HttpClient with timeouts set for http connection
     * and timeouts for ssl connection (tls v1.3).  Note that closeNotifyReadTimeout set to 1 second
     * Also creates/uses a connection pool of max 30 connections
     * @return HttpClient
     */
    @Bean
    public HttpClient getHttpsClientWithTimeouts() {
    	// HttpClient client = HttpClient.create(ConnectionProvider.create("test-webclient-pool", 15))
    	HttpClient client = HttpClient.create()
    			  .wiretap(true)
    			  // 8 second response timeout (socket timeout)
    			  .responseTimeout(Duration.ofSeconds(8))
    			  // 5 second http connection timeout
    			  .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
    			  .option(ChannelOption.SO_KEEPALIVE, true);
    	Http11SslContextSpec http11SslContextSpec = Http11SslContextSpec.forClient().configure(
    			builder -> {
    				builder.trustManager(InsecureTrustManagerFactory.INSTANCE);
//    				try {
//						builder.keyManager(KeyManagerFactory.getInstance("TLSv1.3"));
//					} catch (NoSuchAlgorithmException e) {
//					}
    			});
        client = client.secure(
        		spec -> {
	                spec.sslContext(http11SslContextSpec)
						    .handshakeTimeout(Duration.ofSeconds(10))
						    .closeNotifyFlushTimeout(Duration.ofSeconds(1))
						    // below setting for tls v1.3 will only wait 1 second for close_notify from peer
						    // after sending a close_notify
						    .closeNotifyReadTimeout(Duration.ofSeconds(1));
	        	});
        LOG.debug("Configured HttpsClient with Timeouts: {}", client.toString());
    	return client;
    }
    
    @Bean
    public HttpClient getDefaultHttpsClient() {
    	HttpClient client = HttpClient.create().wiretap(true);
    	Http11SslContextSpec http11SslContextSpec = Http11SslContextSpec.forClient().configure(
    			builder -> {
    				builder.trustManager(InsecureTrustManagerFactory.INSTANCE);
//    				try {
//						builder.keyManager(KeyManagerFactory.getInstance("TLSv1.3"));
//					} catch (NoSuchAlgorithmException e) {
//					}
    			});
        client = client.secure(
        		spec -> {
	                spec.sslContext(http11SslContextSpec);
	        	});
        LOG.debug("Configured Default HttpsClient: {}", client.toString());
    	return client;
    }

    
    @Bean
    @Qualifier("CIWebClientWithTimeouts")
    public WebClient getPooledCIWebClientWithTimeouts() {
    	WebClient client = WebClient.builder()
    			  .baseUrl(ciUrl)
    			  .clientConnector(new ReactorClientHttpConnector(getHttpsClientWithTimeouts()))
    			  .build();
    	LOG.debug("Built CI Web Client With Timeouts");
    	return client;
    }
    
    @Bean
    @Qualifier("DefaultCIWebClient")
    public WebClient getDefaultCIWebClient() {
    	return WebClient.builder()
  			  .baseUrl(ciUrl)
  			  .clientConnector(new ReactorClientHttpConnector(getDefaultHttpsClient()))
  			  .build();
    }
    
    @Bean
    @Qualifier("CommPermWebClientWithTimeouts")
    public WebClient getPooledCommPermWebClientWithTimeouts() {
    	WebClient client = WebClient.builder()
    			  .baseUrl(commPermUrl)
    			  .clientConnector(new ReactorClientHttpConnector(getHttpsClientWithTimeouts()))
    			  .build();
    	LOG.debug("Built CommPerm Web Client With Timeouts");
    	return client;
    }
    
    @Bean
    @Qualifier("DefaultCommPermWebClient")
    public WebClient getDefaultCommPermWebClient() {
    	return WebClient.builder()
    			  .baseUrl(commPermUrl)
    			  .clientConnector(new ReactorClientHttpConnector(getDefaultHttpsClient()))
    			  .build();
    }
    
    @Bean
    @Qualifier("ZombieWebClientWithTimeouts")
    public WebClient getZombieWebClientWithTimeouts() {
    	WebClient client = WebClient.builder()
    			  .baseUrl(zombieUrl)
    			  .clientConnector(new ReactorClientHttpConnector(getHttpsClientWithTimeouts()))
    			  .build();
    	LOG.debug("Built Zombie Web Client With Timeouts");
    	return client;
    }
    
    @Bean
    @Qualifier("DefaultZombieWebClient")
    public WebClient getDefaultZombieWebClient() {
    	return WebClient.builder()
    			  .baseUrl(zombieUrl)
    			  .clientConnector(new ReactorClientHttpConnector(getDefaultHttpsClient()))
    			  .build();
    }

}

