package com.cs2trade.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;

/**
 * RestTemplate配置类
 * 配置跳过SSL证书验证（仅用于开发测试环境）
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Configuration
public class RestTemplateConfig {

    @Value("${steam.proxy.enabled:false}")
    private boolean proxyEnabled;

    @Value("${steam.proxy.host:127.0.0.1}")
    private String proxyHost;

    @Value("${steam.proxy.port:7890}")
    private int proxyPort;

    @Value("${steam.proxy.type:HTTP}")
    private String proxyType;

    /**
     * 创建跳过SSL验证的RestTemplate
     * 注意：仅用于开发测试环境，生产环境请勿使用
     */
    @Bean
    public RestTemplate restTemplate() throws Exception {
        // 创建跳过SSL证书验证的SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[]{};
            }
        }}, new java.security.SecureRandom());

        // 创建跳过主机名验证的HostnameVerifier
        HostnameVerifier hostnameVerifier = (hostname, session) -> true;

        // 配置SSL连接
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);

        // 创建RestTemplate
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 缩短超时时间，避免前端请求超时
        factory.setConnectTimeout(3000); // 3秒连接超时
        factory.setReadTimeout(5000);    // 5秒读取超时

        if (proxyEnabled) {
            java.net.Proxy.Type type = "SOCKS".equalsIgnoreCase(proxyType) ? java.net.Proxy.Type.SOCKS : java.net.Proxy.Type.HTTP;
            java.net.Proxy proxy = new java.net.Proxy(type, new java.net.InetSocketAddress(proxyHost, proxyPort));
            factory.setProxy(proxy);
        }

        return new RestTemplate(factory);
    }
}
