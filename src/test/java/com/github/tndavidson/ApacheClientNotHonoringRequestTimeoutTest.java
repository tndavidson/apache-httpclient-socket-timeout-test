package com.github.tndavidson;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class ApacheClientNotHonoringRequestTimeoutTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApacheClientNotHonoringRequestTimeoutTest.class);

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
    public void testApacheClientRequestTimeout() throws IOException, URISyntaxException {
        var config = new HttpClientConfig();
        config.setRequestTimeout(8000);
        ClientUtil.setTlsProps(config);

        var httpClient = ClientUtil.buildHttpClient(config);

        WireMock.stubFor(
                get(WireMock.urlMatching("/test-tls")).willReturn(aResponse().withFixedDelay(20000)));

        var httpget = new HttpGet("https://localhost:8443/test-tls");
        var startTime = System.currentTimeMillis();

        try {
            httpClient.execute(httpget, (HttpClientResponseHandler<String>) classicHttpResponse -> null);
        } catch (SocketTimeoutException e) {
            LOGGER.error("Socket Timeout", e);
        }

        var endTime = System.currentTimeMillis();

        Assertions.assertThat(endTime - startTime).isLessThan(9000L);
    }
}
