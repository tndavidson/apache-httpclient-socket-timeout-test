package com.github.tndavidson;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static java.net.URI.create;

public class JavaNativeHttpClientMtlsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaNativeHttpClientMtlsTest.class);

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().httpsPort(8446)
            .keystorePath("./src/test/resources/server-keystore.jks").keystorePassword("secret").keyManagerPassword("secret")
            .trustStorePath("./src/test/resources/server-truststore.jks").trustStorePassword("secret").httpDisabled(true));


    @Test
    public void testJavaNativeHttpClientMtls() throws URISyntaxException, IOException, InterruptedException {

        System.setProperty("jdk.httpclient.HttpClient.log", "errors,requests,headers,frames[:control:data:window:all],content,ssl,trace,channel,all");

        HttpClientConfig config = new HttpClientConfig();
        config.setProtocol("TLS");
        config.setKeyStore(
                Paths.get(Thread.currentThread().getContextClassLoader().getResource("client-keystore.jks").toURI())
                        .toString());
        config.setKeyStorePassword("secret");
        config.setKeyAlias("secure-client");
        config.setKeyPassword("secret");
        config.setTrustStore(
                Paths.get(Thread.currentThread().getContextClassLoader().getResource("client-truststore.jks").toURI())
                        .toString());
        config.setTrustStorePassword("secret");

        var sslContext = HttpClientUtil.sslContext(config);

        final HttpClient httpClient = HttpClient.newBuilder()
                .sslContext(sslContext)
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        WireMock.stubFor(
                get(WireMock.urlMatching("/fiduciary-data/v2/123456789")).willReturn(aResponse().withFixedDelay(20000)));

        // because the CN of the server cert is "localhostServer", you need to add the following entry in your /etc/hosts file (C:\Windows\System32\drivers\etc\hosts on windows)
        // 127.0.0.1       localhostServer

        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(create("https://localhostServer:8446/fiduciary-data/v2/123456789")).timeout(Duration.ofSeconds(8)).build();

        LOGGER.info("sending GET request to wiremock...");
        HttpResponse<String> response = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());

        LOGGER.info("response: {}", response);
    }




}
