package com.github.tndavidson;

import java.util.List;

public class HttpClientConfig {
    /** The pool name is used in select places, such as in custom metrics. */
    private String poolName;

    /** connection related properties*/
    private int connectionRequestTimeout = 5000;
    private int connectTimeout = 2000;
    private int socketTimeout = 15000;
    private int connectionTimeToLive = 5000;
    private int maxConnPerRoute = 300;
    private int maxConnTotal = 500;
    /** ssl related properties */
    private String keyAlias;
    private String keyPassword;
    private String keyStore;
    private String keyStorePassword;
    private String trustStore;
    private String trustStorePassword;
    private List<String> cipherSuites = List.of("TLS_AES_256_GCM_SHA384");
    private String protocol = "TLS";
    private boolean staleConnectionCheckEnabled = false;
    private int maxIdleTime;


    public int getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    public HttpClientConfig setConnectionRequestTimeout(int connectionRequestTimeout) {
        this.connectionRequestTimeout = connectionRequestTimeout;
        return this;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public HttpClientConfig setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public HttpClientConfig setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
        return this;
    }

    public int getConnectionTimeToLive() {
        return connectionTimeToLive;
    }

    public HttpClientConfig setConnectionTimeToLive(int connectionTimeToLive) {
        this.connectionTimeToLive = connectionTimeToLive;
        return this;
    }

    public int getMaxConnPerRoute() {
        return maxConnPerRoute;
    }

    public HttpClientConfig setMaxConnPerRoute(int maxConnPerRoute) {
        this.maxConnPerRoute = maxConnPerRoute;
        return this;
    }

    public int getMaxConnTotal() {
        return maxConnTotal;
    }

    public HttpClientConfig setMaxConnTotal(int maxConnTotal) {
        this.maxConnTotal = maxConnTotal;
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

    public List<String> getCipherSuites() {
        return cipherSuites;
    }

    public HttpClientConfig setCipherSuites(List<String> cipherSuites) {
        this.cipherSuites = cipherSuites;
        return this;
    }


    public String getProtocol() {
        return protocol;
    }

    public HttpClientConfig setProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public boolean isStaleConnectionCheckEnabled() {
        return staleConnectionCheckEnabled;
    }

    public HttpClientConfig setStaleConnectionCheckEnabled(boolean staleConnectionCheckEnabled) {
        this.staleConnectionCheckEnabled = staleConnectionCheckEnabled;
        return this;
    }

    public int getMaxIdleTime() {
        return maxIdleTime;
    }

    public HttpClientConfig setMaxIdleTime(int maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
        return this;
    }
}
