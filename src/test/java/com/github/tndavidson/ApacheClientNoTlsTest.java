package com.github.tndavidson;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.junit.Assert.assertNotNull;

public class ApacheClientNoTlsTest {

private static final Logger LOGGER = LoggerFactory.getLogger(ApacheClientNoTlsTest.class);

	@Rule
	public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().port(8081));

	/**
	 * This is the same scenario without using TLS, this test will pass, and the client honors the 8-second request
	 * timeout
	 *
	 * @throws IOException
	 */
	//@Test
	public void testApacheNoTls() throws IOException {
		
		var config = new HttpClientConfig();
		config.setRequestTimeout(8000);
		
		HttpClient httpClient = ClientUtil.buildHttpClient(config);
		
		WireMock.stubFor(
				get(WireMock.urlMatching("/test-no-tls")).willReturn(aResponse().withFixedDelay(20000)));
		
		HttpGet httpget = new HttpGet("http://localhost:8081/test-no-tls");
		var startTime = System.currentTimeMillis();

		try {
			
			httpClient.execute(httpget, (HttpClientResponseHandler<String>) classicHttpResponse -> null);
		} catch (Exception e) {
			LOGGER.error("Socket Timeout", e);
		}

		var endTime = System.currentTimeMillis();
		System.out.println("*******  apache no tls total time (millis): " + Long.toString(endTime - startTime) + "  ******");
		Assertions.assertThat(endTime - startTime).isLessThan(9000L);
	}
}
