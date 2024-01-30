package com.github.tndavidson;


import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.commons.io.FilenameUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContexts;

public class ClientUtil {

    public static HttpClient buildHttpClient(HttpClientConfig config) {
        var requestConfig = RequestConfig.custom()
                .setResponseTimeout(config.getRequestTimeout(), TimeUnit.MILLISECONDS)
                .build();
        var httpClientBuilder = HttpClients.custom().setDefaultRequestConfig(requestConfig);
        var socketFactoryRegistryBuilder = RegistryBuilder.<ConnectionSocketFactory>create();

        if ("TLS".equals(config.getProtocol())) {
            var sslContext = sslContext(config);
            var sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
            socketFactoryRegistryBuilder.register("https", sslConnectionSocketFactory);
        } else {
            socketFactoryRegistryBuilder.register("http", new PlainConnectionSocketFactory());
        }

        var socketFactoryRegistry = socketFactoryRegistryBuilder.build();
        var httpClientManager = new BasicHttpClientConnectionManager(socketFactoryRegistry);

        httpClientBuilder.setConnectionManager(httpClientManager);

        return httpClientBuilder.build();
    }

    public static void setTlsProps(HttpClientConfig config) throws URISyntaxException {
        config.setProtocol("TLS");
        config.setKeyStore(Paths.get(Thread.currentThread().getContextClassLoader()
                .getResource("client-keystore.jks").toURI()).toString());
        config.setKeyStorePassword("secret");
        config.setKeyAlias("secure-client");
        config.setKeyPassword("secret");
        config.setTrustStore(Paths.get(Thread.currentThread().getContextClassLoader()
                .getResource("client-truststore.jks").toURI()).toString());
        config.setTrustStorePassword("secret");
    }

    public static SSLContext sslContext(final HttpClientConfig config) {
        var sslContextBuilder = SSLContexts.custom().setProtocol(config.getProtocol());

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