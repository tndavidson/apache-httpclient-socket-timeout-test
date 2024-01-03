package com.github.tndavidson;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.TimeValue;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;

public class HttpClientUtil {


    public static HttpClient buildHttpClient(HttpClientConfig config) {
        final RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(config.getConnectionRequestTimeout(), TimeUnit.MILLISECONDS)
                .setConnectTimeout(config.getConnectTimeout(), TimeUnit.MILLISECONDS)
                .setResponseTimeout(config.getSocketTimeout(), TimeUnit.MILLISECONDS)
                .build();

        HttpClientBuilder httpClientBuilder = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig);

        PoolingHttpClientConnectionManagerBuilder connectionManagerBuilder = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnPerRoute(config.getMaxConnPerRoute())
                .setMaxConnTotal(config.getMaxConnTotal())
//				.setDefaultSocketConfig(SocketConfig.custom()
//						.setSoTimeout(Timeout.of(httpClientProperties.getSocketTimeout(), TimeUnit.MILLISECONDS))
//						.build())
                .setConnectionTimeToLive(TimeValue.of(config.getConnectionTimeToLive(), TimeUnit.MILLISECONDS));

        final String[] cipherSuites = !config.getCipherSuites().isEmpty() ?
                config.getCipherSuites().toArray(String[]::new) : null;

        SSLContext sslContext = null;
        if (StringUtils.isNotEmpty(config.getKeyStore())) {
            sslContext = sslContext(config);
        }
        if (sslContext != null) {
            connectionManagerBuilder.setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext, null, cipherSuites, NoopHostnameVerifier.INSTANCE));
        }
        if (config.getMaxIdleTime() > 0) {
            httpClientBuilder = httpClientBuilder.evictIdleConnections(TimeValue.of(config.getMaxIdleTime(), TimeUnit.MILLISECONDS));
        }
        PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = connectionManagerBuilder.build();
        httpClientBuilder.setConnectionManager(poolingHttpClientConnectionManager);

        return httpClientBuilder.build();
    }

    public static SSLContext sslContext(final HttpClientConfig config) {
        final SSLContextBuilder sslContextBuilder = SSLContexts.custom().setProtocol(config.getProtocol());

        try {
            sslContextBuilder
                    .loadKeyMaterial(new File(FilenameUtils.normalize(config.getKeyStore())),
                            config.getKeyStorePassword().toCharArray(),
                            config.getKeyPassword().toCharArray(),
                            (aliases, socket) -> config.getKeyAlias());

            if (config.getTrustStore() != null) {
                sslContextBuilder.loadTrustMaterial(new File(FilenameUtils.normalize(config.getTrustStore())),
                        config.getTrustStorePassword().toCharArray());
            }
            return sslContextBuilder.build();
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
