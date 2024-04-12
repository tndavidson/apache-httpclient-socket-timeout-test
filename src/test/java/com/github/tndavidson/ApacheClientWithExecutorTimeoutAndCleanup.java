package com.github.tndavidson;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class ApacheClientWithExecutorTimeoutAndCleanup {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApacheClientWithExecutorTimeoutAndCleanup.class);

//    @Rule
//    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().httpsPort(8443)
//            .keystorePath("./src/test/resources/server-keystore.jks")
//            .keystorePassword("secret")
//            .keyManagerPassword("secret")
//            .trustStorePath("./src/test/resources/server-truststore.jks")
//            .trustStorePassword("secret")
//            .httpDisabled(true));
    
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().port(8443));
    
	@Rule
	public WireMockRule wireMockNoTlsRule = new WireMockRule(WireMockConfiguration.wireMockConfig().port(8081));

    /**
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test
    public void testApacheMultipleClientsWithTimeout() throws IOException, URISyntaxException {
    	System.out.println("Start testApacheMultipleClientsWithTimeout");
    	wireMockRule.stubFor(
                get(WireMock.urlMatching("/test-tls")).willReturn(aResponse().withFixedDelay(20000)));
        
    	wireMockNoTlsRule.stubFor(
				get(WireMock.urlMatching("/test-no-tls")).willReturn(aResponse().withBody("We Good Here")));
		
		List<String> aggregatedResponse = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(5);
        
        List<Future<String>> futureList = new ArrayList<>();
		try {
			// Timeout of 5 seconds
			futureList = executor.invokeAll(Arrays.asList(
					new HttpCallable(), new HttpCallable(), new HttpsHangingCallable()), 5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			System.out.println("InterruptedException Fired From Executor Invoke");
		}
        long startTime = System.currentTimeMillis();

        try {
	        for(Future<String> f : futureList) {
	
	            if (!f.isCancelled()) {
					System.out.println("Success : " + f.get());
	                aggregatedResponse.add(f.get());
	            } else {
	                System.out.println("Timeout Fired and No Response Yet");
	                aggregatedResponse.add("No Response From Zombie Partner");
	                f.cancel(true);
	            }
	        }
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        var endTime = System.currentTimeMillis();
        System.out.println("*******  aggregate total time (millis): " + Long.toString(endTime - startTime) + "  ******");
        System.out.println("*******  aggregate response: " + aggregatedResponse.toString() + "  ******");

    	
        executor.shutdown();

    }
    

    static class HttpCallable implements Callable<String>
    {

        public String call() throws Exception {
        	String responseStr = null;
            long startTime = System.currentTimeMillis();
            
            try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            	HttpGet httpget = new HttpGet("http://localhost:8081/test-no-tls");
                try (CloseableHttpResponse response = httpclient.execute(httpget)) {
                    // Get status code
                    System.out.println(response.getVersion()); // HTTP/1.1
                    System.out.println(response.getCode()); // 200
                    System.out.println(response.getReasonPhrase()); // OK
                    HttpEntity entity = response.getEntity();
                    // Get response information
                    responseStr = EntityUtils.toString(entity);
                }
            } catch (Exception e) {
            	LOGGER.error("HttpCallable Exception: ", e);
            }

            long endTime = System.currentTimeMillis();
            LOGGER.debug("apache http client total time (millis): " + Long.toString(endTime - startTime));

            return responseStr;
        	
        }
    }

    static class HttpsHangingCallable implements Callable<String>
    {

        public String call() throws Exception {
//        	String response = null;
//        	HttpClientConfig config = new HttpClientConfig();
//            config.setRequestTimeout(8000);
//            ClientUtil.setTlsProps(config);
//
//            HttpClient httpClient = ClientUtil.buildHttpClient(config);
//
//            HttpGet httpsget = new HttpGet("http://localhost:8443/test-tls");
//            long startTime = System.currentTimeMillis();
//
//            try {
//                response = httpClient.execute(httpsget, (HttpClientResponseHandler<String>) classicHttpResponse -> null);
//                
//            } catch (Exception e) {
//            	LOGGER.error("HttpsHangingCallable Exception: ", e);
//            }
            
            
            
        	String responseStr = null;
            long startTime = System.currentTimeMillis();
            
            try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            	HttpGet httpget = new HttpGet("http://localhost:8443/test-tls");
                try (CloseableHttpResponse response = httpclient.execute(httpget)) {
                    // Get status code
                    System.out.println(response.getVersion()); // HTTP/1.1
                    System.out.println(response.getCode()); // 200
                    System.out.println(response.getReasonPhrase()); // OK
                    HttpEntity entity = response.getEntity();
                    // Get response information
                    responseStr = EntityUtils.toString(entity);
                }
            } catch (Exception e) {
            	LOGGER.error("HttpCallable Exception: ", e);
            }
            
            
            

            var endTime = System.currentTimeMillis();
            LOGGER.debug("apache hanging http total time (millis): " + Long.toString(endTime - startTime));

            return responseStr;
        }
    }

}
