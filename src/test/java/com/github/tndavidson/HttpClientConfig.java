package com.github.tndavidson;

public class HttpClientConfig {
    private int requestTimeout;
    private String keyAlias = "secure-client";
    private String keyPassword = "secret";
    private String keyStore;
    private String keyStorePassword;
    private String trustStore;
    private String trustStorePassword;
    private String protocol;

    public int getRequestTimeout() {
        return requestTimeout;
    }

    public HttpClientConfig setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    public String getKeyAlias() {
        return keyAlias;
    }

    public HttpClientConfig setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
        return this;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public HttpClientConfig setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
        return this;
    }

    public String getKeyStore() {
        return keyStore;
    }

    public HttpClientConfig setKeyStore(String keyStore) {
        this.keyStore = keyStore;
        return this;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public HttpClientConfig setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
        return this;
    }

    public String getTrustStore() {
        return trustStore;
    }

    public HttpClientConfig setTrustStore(String trustStore) {
        this.trustStore = trustStore;
        return this;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public HttpClientConfig setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
        return this;
    }

    public String getProtocol() {
        return protocol;
    }

    public HttpClientConfig setProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }
}
