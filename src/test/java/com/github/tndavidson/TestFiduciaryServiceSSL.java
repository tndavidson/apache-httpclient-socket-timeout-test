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
import java.net.URISyntaxException;
import java.nio.file.Paths;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.junit.Assert.assertNotNull;

public class TestFiduciaryServiceSSL {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TestFiduciaryServiceSSL.class);
	
	HttpClientConfig config = new HttpClientConfig();

	@Rule
	public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().httpsPort(8446)
			.keystorePath("./src/test/resources/server-keystore.jks").keystorePassword("secret").keyManagerPassword("secret")
			.trustStorePath("./src/test/resources/server-truststore.jks").trustStorePassword("secret").httpDisabled(true));


	//WireMockServer wiremockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().httpsPort(8446));
	
    @SuppressWarnings("deprecation")
	@Test
	public void TestFiduciary() throws IOException, URISyntaxException {
		config.setConnectionRequestTimeout(10000);
		config.setConnectionTimeToLive(10000);
		config.setConnectTimeout(10000);
		config.setSocketTimeout(8000);
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
		

		WireMock.stubFor(
				get(WireMock.urlMatching("/fiduciary-data/v2/123456789")).willReturn(aResponse().withFixedDelay(20000)));
		
		
		
		HttpGet httpget = new HttpGet("https://localhost:8446/fiduciary-data/v2/123456789");
		 
		 HttpResponse response= httpClient.execute(httpget);
		 
		LOGGER.info("Response: " + response);
		assertNotNull(response);

	}









}