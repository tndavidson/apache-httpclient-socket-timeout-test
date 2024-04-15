package com.github.tndavidson;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;

import java.io.IOException;
import java.lang.Thread.State;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.reactor.IOReactorStatus;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;


public class ApacheClientNotHonoringRequestTimeoutTest {
	
    private static final Logger LOG = LoggerFactory.getLogger(ApacheClientNotHonoringRequestTimeoutTest.class);

    private static final SimpleResponseHandler handler = new SimpleResponseHandler();
    
    // threadList is just used to get references to callable threads in executor service pool
    // to display their runtime status
    private static final List<Thread> threadList = new ArrayList<>();

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().httpsPort(8443)
            .keystorePath("./src/test/resources/server-keystore.jks")
            .keystorePassword("secret")
            .keyManagerPassword("secret")
            .trustStorePath("./src/test/resources/server-truststore.jks")
            .trustStorePassword("secret")
            .httpDisabled(true));

    /**
     * This test reproduces the issue I am running into in my application. You can see by looking at the logs,
     * that it appears there is a read timeout happening after 8 seconds (configured request timeout), then it appears
     * to block another 8 seconds while closing the connection
     * <p>
     * if you change the request timeout to 4 seconds, it will block an additional 4 seconds while closing
     * the connection
     *
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test
    public void testApacheSynchronousClientRequestTimeoutWithZombie() throws IOException, URISyntaxException {
    	ExecutorService executor = Executors.newFixedThreadPool(10);
        WireMock.stubFor(
                get(WireMock.urlMatching("/test-tls")).willReturn(aResponse().withHeader("Content-Type", "text/plain")
                        .withBody("We Good")));
        WireMock.stubFor(
                get(WireMock.urlMatching("/test-tls-zombie")).willReturn(aResponse().withFixedDelay(20000)));
		List<String> aggregatedResponse = new ArrayList<>();
        
        List<Future<String>> futureList = new ArrayList<>();
        long startTime = System.currentTimeMillis();
		try {
			LOG.debug("Executor Invocation Start");
			// Timeout of 5 seconds
			futureList = executor.invokeAll(Arrays.asList(new HttpsSyncCallable(handler), new HttpsSyncCallable(handler), 
					new HttpsSyncHangingCallable(handler)), 5000L, TimeUnit.MILLISECONDS);
			LOG.debug("Executor Invocation Completed");
	        futureList.stream().forEach(f -> {
	            if (f.isCancelled()) {
	            	LOG.debug("Timeout Fired and No Response Yet");
	                aggregatedResponse.add("No Response From Zombie Partner");
	                // you can do f.cancel(true) or get reference to thread (using threadList)
	                // and do thread.interupt() but nothing works to kill thread blocked in ssl io (been there tried it)
	            } else {
	            	try {
		            	LOG.debug("Future completed and returned : " + f.get());
		                aggregatedResponse.add(f.get());
	            	} catch (InterruptedException | ExecutionException e) {
	            	}
	            }
	        });
		} catch (InterruptedException e) {
			LOG.error("InterruptedException Fired From Executor Invoke");
		}
        long endTime = System.currentTimeMillis();
        System.out.println("*******  aggregate total time (millis): " + Long.toString(endTime - startTime) + "  ******");
        System.out.println("*******  aggregate response: " + aggregatedResponse.toString() + "  ******");

        // sleep/wait to continue getting logs and thread status
        monitorThreadPoolThreadsTillCleared();
        System.out.println("*******  total time for all threads to clear (millis): " + Long.toString(System.currentTimeMillis() - startTime) + "  ******");
        finalDumpAndClearThreadList();
        executor.shutdown();
    }
    
    @Test
    public void testApacheAsyncClientRequestTimeoutWithZombie() throws IOException, URISyntaxException {
    	ExecutorService executor = Executors.newFixedThreadPool(10);
        WireMock.stubFor(
                get(WireMock.urlMatching("/test-tls")).willReturn(aResponse().withHeader("Content-Type", "text/plain")
                        .withBody("We Good")));
        WireMock.stubFor(
                get(WireMock.urlMatching("/test-tls-zombie")).willReturn(aResponse().withFixedDelay(20000)));
		List<String> aggregatedResponse = new ArrayList<>();
        
        List<Future<String>> futureList = new ArrayList<>();
        long startTime = System.currentTimeMillis();
		try {
			LOG.debug("Executor Invocation Start");
			// Timeout of 5 seconds
			futureList = executor.invokeAll(Arrays.asList(new AsyncHttpsCallable(handler), new AsyncHttpsCallable(handler), 
					new AsyncHttpsHangingCallable(handler)), 5000L, TimeUnit.MILLISECONDS);
			LOG.debug("Executor Invocation Completed");
			try {
		        for(Future<String> f : futureList) {
		            if (f.isCancelled()) {
		            	LOG.debug("Timeout Fired and No Response Yet");
		                aggregatedResponse.add("No Response From Zombie Partner");
		            } else {
			            LOG.debug("Future completed and returned : " + f.get());
			            aggregatedResponse.add(f.get());
		            }
		        }
        	} catch (ExecutionException e) {
        	}
		} catch (InterruptedException e) {
			LOG.error("InterruptedException Fired From Executor Invoke");
		}
		
        long endTime = System.currentTimeMillis();
        System.out.println("*******  aggregate total time (millis): " + Long.toString(endTime - startTime) + "  ******");
        System.out.println("*******  aggregate response: " + aggregatedResponse.toString() + "  ******");

        // sleep/wait to continue getting logs and thread status
        monitorThreadPoolThreadsTillCleared();
        System.out.println("*******  total time for all threads to clear (millis): " + Long.toString(System.currentTimeMillis() - startTime) + "  ******");
        finalDumpAndClearThreadList();
        executor.shutdown();
    }
    
    
    
    static class HttpsSyncCallable implements Callable<String>
    {
    	SimpleResponseHandler handler;
    	public HttpsSyncCallable(SimpleResponseHandler h) {
    		handler = h;
    	}

        public String call() throws Exception {
        	threadList.add(Thread.currentThread());
        	
        	String responseStr = null;
            var config = new HttpClientConfig();
            config.setRequestTimeout(8000);
            ClientUtil.setTlsProps(config);
            var httpClient = ClientUtil.buildHttpClient(config);
            var httpget = new HttpGet("https://localhost:8443/test-tls");

            try {
            	System.out.println("******* sync client calling https://localhost:8443/test-tls ******");
            	responseStr = httpClient.execute(httpget, handler);
            } catch (Exception e) {
                LOG.error("Callable Exception: ", e);
            }
            return responseStr;
        }
    }

    static class HttpsSyncHangingCallable implements Callable<String>
    {
    	SimpleResponseHandler handler;
    	public HttpsSyncHangingCallable(SimpleResponseHandler h) {
    		handler = h;
    	}
    	
        public String call() throws Exception {
        	threadList.add(Thread.currentThread());
        	String responseStr = null;
            var config = new HttpClientConfig();
            config.setRequestTimeout(8000);
            ClientUtil.setTlsProps(config);
            var httpClient = ClientUtil.buildHttpClient(config);
            var httpget = new HttpGet("https://localhost:8443/test-tls-zombie");

            try {
            	System.out.println("******* sync client calling https://localhost:8443/test-tls-zombie ******");
            	responseStr = httpClient.execute(httpget, handler);
            } catch (Exception e) {
                LOG.error("Callable Exception: ", e);
            }
            return responseStr;
        }
    }

    
    static class AsyncHttpsHangingCallable implements Callable<String>
    {
    	SimpleResponseHandler handler;
    	public AsyncHttpsHangingCallable(SimpleResponseHandler h) {
    		handler = h;
    	}
    	
        public String call() throws Exception {
        	threadList.add(Thread.currentThread());
        	String responseStr = null;
        	
            TrustStrategy acceptingTrustStrategy = (certificate, authType) -> true;

            SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();

            TlsStrategy tlsStrategy = ClientTlsStrategyBuilder.create()
                .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .setSslContext(sslContext)
                .build();

            PoolingAsyncClientConnectionManager cm = PoolingAsyncClientConnectionManagerBuilder.create()
                .setTlsStrategy(tlsStrategy)
                .build();

            CloseableHttpAsyncClient client = HttpAsyncClients.custom()
                .setConnectionManager(cm)
                .build();

            client.start();

            SimpleHttpRequest request = new SimpleHttpRequest("GET", "https://localhost:8443/test-tls-zombie");
            System.out.println("******* async client calling https://localhost:8443/test-tls-zombie ******");
            Future<SimpleHttpResponse> future = client.execute(request, null);

            LOG.debug("GET request execute returned with future");
            try {
	            SimpleHttpResponse response = future.get();
	            // will never get here as blocking/waiting for response that never comes
	            LOG.debug("future get() returned");
	            responseStr = response.getBodyText();
            } catch(Exception e) {
            	// catch the interrupted exception that is thrown so that
            	// and ensure we close the client
            	LOG.error("Async Client was interrupted");
            } finally {
	            LOG.debug("closing client");
	            client.close();
            }
            return responseStr;
        }
    }
    
    static class AsyncHttpsCallable implements Callable<String>
    {
    	SimpleResponseHandler handler;
    	public AsyncHttpsCallable(SimpleResponseHandler h) {
    		handler = h;
    	}
    	
        public String call() throws Exception {
        	threadList.add(Thread.currentThread());
        	String responseStr = null;
        	
            TrustStrategy acceptingTrustStrategy = (certificate, authType) -> true;
            SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();
            TlsStrategy tlsStrategy = ClientTlsStrategyBuilder.create()
                .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .setSslContext(sslContext)
                .build();
            PoolingAsyncClientConnectionManager cm = PoolingAsyncClientConnectionManagerBuilder.create()
                .setTlsStrategy(tlsStrategy)
                .build();
            CloseableHttpAsyncClient client = HttpAsyncClients.custom()
                .setConnectionManager(cm)
                .build();
            client.start();

            SimpleHttpRequest request = new SimpleHttpRequest("GET", "https://localhost:8443/test-tls");
            System.out.println("******* async client calling https://localhost:8443/test-tls ******");
            Future<SimpleHttpResponse> future = client.execute(request, null);
            try {
	            SimpleHttpResponse response = future.get();
	            responseStr = response.getBodyText();
            }catch(Exception e) {
            	LOG.error("Async Client exception: ", e);
            } finally {
	            LOG.debug("closing client");
	            client.close();
            }
            return responseStr;
        }
    }
    
//    private void forceCloseIfNecessary() {
//        // Stop the underlying thread that is still running/hanging even though Callable was cancelled
//		// the underlying callable thread is hung waiting on the https/io to complete.
//        // So, we force close the https socket by doing immediate close of the CloseableHttpAsyncClient.
//        // With the io unblocked, the thread is interrupted and returns to pool.
//    	ListIterator<CloseableHttpAsyncClient> iter = hangingClientList.listIterator();
//    	while(iter.hasNext()){
//    		CloseableHttpAsyncClient client = iter.next();
//			LOG.debug("hangingClientList has CloseableAsyncClient with status {}", client.getStatus());
//			if(client.getStatus() == IOReactorStatus.ACTIVE) {
//				LOG.debug("CloseableAsyncClient still running so killing it");
//				client.close(CloseMode.IMMEDIATE);
//			}
//			iter.remove();
//    	}
//    }
   
    private void monitorThreadPoolThreadsTillCleared() {
        boolean threadsRunning = true;
        while(threadsRunning) {
        	threadsRunning = false;
        	for(Thread t : threadList) {
        		LOG.debug("Thread {} has status {}", t.getName(), t.getState().toString());
        		if(t.getState() == State.RUNNABLE || t.getState() == State.BLOCKED) {
        			LOG.debug("Still Have Running Thread {} with status {}", t.getName(), t.getState().toString());
        			threadsRunning = true;
        		} else if(t.getState() == State.TIMED_WAITING) {
        			LOG.debug("Have TIMED_WAITING Thread {} with status {}", t.getName(), t.getState().toString());
        			threadsRunning = true;
        		}
        	}
        	if(threadsRunning) {
			    try {
					Thread.sleep(500L);
				} catch (InterruptedException e) {
				}
        	}
        }
    }
    
    private void finalDumpAndClearThreadList() {
    	for(Thread t : threadList) {
    		LOG.debug("Thread {} has status {}", t.getName(), t.getState().toString());
    	}
        threadList.clear();
    }
    
    static class SimpleResponseHandler implements HttpClientResponseHandler<String> {
		@Override
		public String handleResponse(ClassicHttpResponse response) throws HttpException, IOException {
			HttpEntity entity = response.getEntity();
			return EntityUtils.toString(entity);
		}
    }
    
}
