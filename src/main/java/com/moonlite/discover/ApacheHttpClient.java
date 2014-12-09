package com.moonlite.discover;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;


/**
 * Use Apache's HttpClient for GET method
 * @author dli
 *
 */
public class ApacheHttpClient {
    private ResponseHandler<String> respHandler = new ResponseHandler<String>() {
        @Override
        public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
            final int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                final HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity) : null;
            } else {
                throw new ClientProtocolException("Unexpected response status: " + status);
            }
        }
    };

    /**
     * Use HttpCLient from Apache, which is even more simpler. Similar to it in JavaScript
     * 
     * @param path String
     * @return String
     * @throws IOException
     */
     public String get(String path) throws IOException {
         return get(path, HttpClients.createDefault(), respHandler);
    }

    /**
     * Use HttpClient but not using responseHandler.
     * @param path String
     * @return String
     * @throws IOException
     */
    public String get2(String path) throws IOException {
        return get(path, HttpClients.createDefault());
    }

    /***
     * This is a https get request that bypasses certificate checking and hostname verifier.
     * It uses basis authentication method.
     * It is tested with Apache httpclient-4.4.
     * It dumps the contents of a https page on the console output.
     * It is very similar to http get request, but with the additional customization of
     *   - credential provider, and
     *   - SSLConnectionSocketFactory to bypass certification checking and hostname verifier.
     * @param path String
     * @param username String
     * @param password String
     * @return String
     * @throws IOException
     */
    public String get(String path, String username, String password) throws IOException {
        final CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCredentialsProvider(createCredsProvider(username, password))
                .setSSLSocketFactory(createGenerousSSLSocketFactory())
                .build();
        return get(path, httpClient, respHandler);
    }

    /**
     * It is preferable to use ResponseHandler when running HttpClient.execute in order to avoid buffer content in memory.
     * @param path
     * @param httpClient
     * @param respHandler
     * @return String
     * @throws IOException
     */
    private String get(String path, CloseableHttpClient httpClient, ResponseHandler<String> respHandler) throws IOException {
        try {
            return  httpClient.execute(new HttpGet(path), respHandler);
        } finally {
            httpClient.close();
        }
    }

    private String get(String path, CloseableHttpClient httpClient) throws IOException {
        final CloseableHttpResponse response = httpClient.execute(new HttpGet(path));
        try {
            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity);
        } finally {
            response.close();
            httpClient.close();
        }
    }
    
    private CredentialsProvider createCredsProvider(String username, String password) {
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));
        return credsProvider;
    }

    /***
     * 
     * @return SSLConnectionSocketFactory that bypass certificate check and bypass HostnameVerifier
     */
    private SSLConnectionSocketFactory createGenerousSSLSocketFactory() {
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]{createGenerousTrustManager()}, new SecureRandom());
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        return new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
    }

    private X509TrustManager createGenerousTrustManager() {
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] cert, String s) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] cert, String s) throws CertificateException {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };
    }
}
