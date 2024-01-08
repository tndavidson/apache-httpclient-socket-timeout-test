package com.github.tndavidson;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.HttpResponse;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.junit.Assert.assertNotNull;

public class TestFiduciaryHttpClient {

private static final Logger LOGGER = LoggerFactory.getLogger(TestFiduciaryHttpClient.class);
	
	HttpClientConfig config = new HttpClientConfig();
	
	@Rule
	public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().port(8556));

	@Test
	public void TestFiduciaryNotTLS() throws IOException {
		config.setConnectionRequestTimeout(10000);
		config.setConnectionTimeToLive(10000);
		config.setConnectTimeout(10000);
		config.setSocketTimeout(8000);
		config.setMaxConnPerRoute(1);
		config.setMaxConnTotal(10);
		
		HttpClient httpClient = HttpClientUtil.buildHttpClient(config);
		
		WireMock.stubFor(
				get(WireMock.urlMatching("/fiduciary-data/v2/123456789")).willReturn(aResponse().withFixedDelay(20000)));
		
		HttpGet httpget = new HttpGet("http://localhost:8556/fiduciary-data/v2/123456789");
		 
		 HttpResponse response= httpClient.execute(httpget);
		 
		LOGGER.info("Response: " + response);
		assertNotNull(response);
		
	}
}
