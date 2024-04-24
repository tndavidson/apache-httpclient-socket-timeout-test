package com.github.tndavidson;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

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
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.Http11SslContextSpec;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;



/**
 * Note that all props/timeouts likely can be set using yml
 * Currently setting explicitly in code.
 */
@Configuration
@ComponentScan(basePackages = {"com.github.tndavidson"})
public class HttpWebClientConfig {
    
    private static final Logger LOG = LoggerFactory.getLogger(HttpWebClientConfig.class);
    
    private String ciUrl = "http://localhost:8443/test-tls-ci";
    private String commPermUrl = "http://localhost:8443/test-tls-commperm";
    private String zombieUrl = "http://localhost:8443/test-tls-zombie";
    
    @Bean
    public WireMockServer getWiremockServer() {
    	WireMockServer wireMockServer = new WireMockServer(8443);
    	
//    	WireMockServer wireMockServer = new WireMockServer(WireMockConfiguration.options()
//    			.httpsPort(8443)
//    			.httpDisabled(true)
//                .keystorePath("./src/test/resources/server-keystore.jks")
//                .keystorePassword("secret")
//                .keyManagerPassword("secret")
//                .trustStorePath("./src/test/resources/server-truststore.jks")
//                .trustStorePassword("secret")
//    	);
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
    public HttpClient getPooledHttpsClientWithTimeouts() {
    	ConnectionProvider poolProvider = ConnectionProvider.builder("custom-connection-pool")
		    			                  .maxConnections(30)
		    			                  .pendingAcquireMaxCount(200)
		    			                  .build();
    	HttpClient client = HttpClient.create(poolProvider)
    			  .wiretap(true)
    			  // 5 second http connection timeout
    			  .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
    			  .option(ChannelOption.SO_KEEPALIVE, true)
    			  .option(EpollChannelOption.TCP_KEEPIDLE, 240)
    			  .option(EpollChannelOption.TCP_KEEPINTVL, 60)
    			  // http read and write timeouts of 8 seconds
    			  .doOnConnected(conn -> conn
    			    .addHandlerLast(new ReadTimeoutHandler(8, TimeUnit.SECONDS))
    			    .addHandlerLast(new WriteTimeoutHandler(8)));
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
        LOG.debug("Configured HttpsClient with Timeouts and ConnectionPool");
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
        LOG.debug("Configured Default HttpsClient");
    	return client;
    }
    
    @Bean
    @Qualifier("PooledCIWebClientWithTimeouts")
    public WebClient getPooledCIWebClientWithTimeouts() {
    	WebClient client = WebClient.builder()
    			  .baseUrl(ciUrl)
    			  .clientConnector(new ReactorClientHttpConnector(getPooledHttpsClientWithTimeouts()))
    			  .build();
    	return client;
    }
    
    @Bean
    @Qualifier("DefaultCIWebClient")
    public WebClient getDefaultCIWebClient() {
    	return WebClient.builder()
  			  .baseUrl(ciUrl)
  			  .clientConnector(new ReactorClientHttpConnector(HttpClient.create().wiretap(true)))
  			  .build();
    }
    
    @Bean
    @Qualifier("PooledCommPermWebClientWithTimeouts")
    public WebClient getPooledCommPermWebClientWithTimeouts() {
    	WebClient client = WebClient.builder()
    			  .baseUrl(commPermUrl)
    			  .clientConnector(new ReactorClientHttpConnector(getPooledHttpsClientWithTimeouts()))
    			  .build();
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
    @Qualifier("PooledZombieWebClientWithTimeouts")
    public WebClient getPooledZombieWebClientWithTimeouts() {
    	WebClient client = WebClient.builder()
    			  .baseUrl(zombieUrl)
    			  .clientConnector(new ReactorClientHttpConnector(getPooledHttpsClientWithTimeouts()))
    			  .build();
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

