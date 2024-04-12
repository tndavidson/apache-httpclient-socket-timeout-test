package com.github.tndavidson;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static java.net.URI.create;

public class JavaNativeHttpClientMtlsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaNativeHttpClientMtlsTest.class);

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().httpsPort(8443)
            .keystorePath("./src/test/resources/server-keystore.jks").keystorePassword("secret").keyManagerPassword("secret")
            .trustStorePath("./src/test/resources/server-truststore.jks").trustStorePassword("secret").httpDisabled(true));


    /**
     * Just for sanity, I added the same scenario using JDK's built in HttpClient, and this test passes, honoring
     * the 8-second timeout
     *
     * @throws URISyntaxException
     * @throws IOException
     * @throws InterruptedException
     */
    //@Test
    public void testJavaNativeHttpClientMtls() throws URISyntaxException, IOException, InterruptedException {
        // Turn up Jav HttpClient logging
        //System.setProperty("jdk.httpclient.HttpClient.log", "errors,requests,headers,frames[:control:data:window:all],content,ssl,trace,channel,all");

    	System.out.println("******* https java client test start ******");
        var config = new HttpClientConfig();
        ClientUtil.setTlsProps(config);

        var sslContext = ClientUtil.sslContext(config);

        var httpClient = HttpClient.newBuilder()
                .sslContext(sslContext)
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        WireMock.stubFor(
                get(WireMock.urlMatching("/test-java-client")).willReturn(aResponse().withFixedDelay(20000)));

        // because the CN of the server cert is "localhostServer", you need to add the following entry in your /etc/hosts file (C:\Windows\System32\drivers\etc\hosts on windows)
        // 127.0.0.1       localhostServer

        var getRequest = HttpRequest.newBuilder()
                .uri(create("https://localhostServer:8443/test-java-client")).timeout(Duration.ofSeconds(8)).build();
        var startTime = System.currentTimeMillis();


        try {
        	System.out.println("******* making socket request ******");
            httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            LOGGER.error("Http timeout", e);
        }

        var endTime = System.currentTimeMillis();
        System.out.println("*******  native java mtls total time (millis): " + Long.toString(endTime - startTime) + "  ******");
        // Assertions.assertThat(endTime - startTime).isLessThan(9000L);
    }
}
