package org.kaorimatz.rundeck.jenkins;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;

import java.net.URI;
import java.net.URISyntaxException;

public class DefaultJenkinsClientBuilder {

    private String baseUrl;

    private String userId;

    private String apiToken;

    public DefaultJenkinsClientBuilder(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public DefaultJenkinsClientBuilder setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public DefaultJenkinsClientBuilder setApiToken(String apiToken) {
        this.apiToken = apiToken;
        return this;
    }

    public DefaultJenkinsClient build() throws URISyntaxException {
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(new BasicHttpClientConnectionManager())
                .setConnectionReuseStrategy(NoConnectionReuseStrategy.INSTANCE)
                .setUserAgent(getUserAgent())
                .disableCookieManagement()
                .disableAutomaticRetries()
                .disableRedirectHandling()
                .build();

        URI baseUri = new URI(baseUrl);

        HttpClientContext httpClientContext = HttpClientContext.create();
        if (userId != null && apiToken != null) {
            HttpHost httpHost = new HttpHost(baseUri.getHost(), baseUri.getPort(), baseUri.getScheme());
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(new AuthScope(httpHost), new UsernamePasswordCredentials(userId, apiToken));
            AuthCache authCache = new BasicAuthCache();
            authCache.put(httpHost, new BasicScheme());
            httpClientContext.setCredentialsProvider(credentialsProvider);
            httpClientContext.setAuthCache(authCache);
        }

        return new DefaultJenkinsClient(request -> httpClient.execute(request, httpClientContext), baseUri);
    }

    private String getUserAgent() {
        String title = getClass().getPackage().getImplementationTitle();
        String version = getClass().getPackage().getImplementationVersion();
        return String.format("%s/%s", title, version);
    }
}
