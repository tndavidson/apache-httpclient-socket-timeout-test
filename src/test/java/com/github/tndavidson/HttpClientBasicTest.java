package com.github.tndavidson;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.nio.file.Paths;

public class HttpClientBasicTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientBasicTest.class);

    @Test
    public void createHttpClient() throws URISyntaxException {
        var config = new HttpClientConfig();
        config.setConnectionRequestTimeout(10000);
        config.setConnectionTimeToLive(10000);
        config.setConnectTimeout(10000);
        config.setSocketTimeout(10000);
        config.setMaxConnPerRoute(1);
        config.setMaxConnTotal(10);
        config.setProtocol("TLS");

        config.setKeyStore(Paths.get(Thread.currentThread().getContextClassLoader()
                .getResource("client-keystore.jks").toURI()).toString());
        config.setKeyStorePassword("secret");
        config.setKeyAlias("secure-client");
        config.setKeyPassword("secret");
        config.setTrustStore(Paths.get(Thread.currentThread().getContextClassLoader()
                .getResource("client-truststore.jks").toURI()).toString());
        config.setTrustStorePassword("secret");

        var httpClient = HttpClientUtil.buildHttpClient(config);

        Assertions.assertThat(httpClient).isNotNull();
    }
}
