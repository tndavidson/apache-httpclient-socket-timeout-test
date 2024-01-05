package com.github.tndavidson;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.HttpResponse;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class TestFiduciaryService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TestFiduciaryService.class);
	
	HttpClientConfig config = new HttpClientConfig();

	@Rule
	public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().httpsPort(8446)
			.keystorePath("./src/test/resources/client-keystore.jks").keystorePassword("secret").keyManagerPassword("secret")
			.trustStorePath("./src/test/resources/client-truststore.jks").trustStorePassword("secret").httpDisabled(true));


	WireMockServer wiremockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().httpsPort(8446));
	
    @SuppressWarnings("deprecation")
	@Test
	public void TestFiduciary() throws IOException, URISyntaxException {
		config.setConnectionRequestTimeout(10000);
		config.setConnectionTimeToLive(10000);
		config.setConnectTimeout(10000);
		config.setSocketTimeout(10000);
		config.setMaxConnPerRoute(1);
		config.setMaxConnTotal(10);
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
		config.getCipherSuites();
		HttpClientUtil.sslContext(config);
		HttpClient httpClient = HttpClientUtil.buildHttpClient(config);
		

		wiremockServer.stubFor(
				get(WireMock.urlMatching("/fiduciary-data/v2/123456789")).willReturn(aResponse().withFixedDelay(2000)));
		
		
		
		HttpGet httpget = new HttpGet("https://localhost:8446/fiduciary-data/v2/123456789");
		 
		 HttpResponse response= httpClient.execute(httpget);
		 
		LOGGER.info("Response: " + response);
		assertNotNull(response);

	}









}