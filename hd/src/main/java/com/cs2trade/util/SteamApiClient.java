package com.cs2trade.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class SteamApiClient {

    private static final String STEAM_INVENTORY_URL = "https://steamcommunity.com/inventory/%s/730/2";
    private static final Pattern STEAM_LISTING_HISTORY_PATTERN = Pattern.compile("var\\s+line1\\s*=\\s*(\\[.*?]);", Pattern.DOTALL);

    @Value("${steam.proxy.enabled:false}")
    private boolean proxyEnabled;

    @Value("${steam.proxy.host:127.0.0.1}")
    private String proxyHost;

    @Value("${steam.proxy.port:7890}")
    private int proxyPort;

    @Value("${steam.proxy.type:HTTP}")
    private String proxyType;

    @Value("${steam.cookie.steam-login-secure:}")
    private String steamLoginSecure;

    private OkHttpClient httpClient;

    @PostConstruct
    public void init() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            HostnameVerifier hostnameVerifier = (hostname, session) -> true;

            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier(hostnameVerifier);

            if (proxyEnabled) {
                Proxy.Type type = "SOCKS".equalsIgnoreCase(proxyType) ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
                builder.proxy(new Proxy(type, new InetSocketAddress(proxyHost, proxyPort)));
            }

            httpClient = builder.build();
            log.info("SteamApiClient initialized. proxyEnabled={}, proxyType={}, proxyHost={}, proxyPort={}",
                    proxyEnabled, proxyType, proxyHost, proxyPort);
        } catch (Exception e) {
            log.error("Failed to initialize SteamApiClient", e);
            throw new IllegalStateException("Failed to initialize SteamApiClient", e);
        }
    }

    public JSONObject getInventory(String steamId) {
        JSONArray mergedAssets = new JSONArray();
        JSONArray mergedDescriptions = new JSONArray();
        JSONArray mergedAssetProperties = new JSONArray();
        JSONObject mergedPayload = null;
        String lastAssetId = null;
        int page = 0;

        while (page < 50) {
            StringBuilder urlBuilder = new StringBuilder(String.format(STEAM_INVENTORY_URL, steamId))
                    .append("?l=schinese&count=2000");
            if (lastAssetId != null && !lastAssetId.isBlank()) {
                urlBuilder.append("&last_assetid=").append(lastAssetId);
            }

            String url = urlBuilder.toString();
            log.info("Request Steam inventory page: steamId={}, page={}, lastAssetId={}, url={}",
                    steamId, page + 1, lastAssetId, url);

            JSONObject pagePayload = requestInventoryPage(url);
            if (pagePayload == null) {
                return mergedPayload;
            }

            if (mergedPayload == null) {
                mergedPayload = JSON.parseObject(pagePayload.toJSONString());
            }

            appendAll(mergedAssets, pagePayload.getJSONArray("assets"));
            appendAll(mergedDescriptions, pagePayload.getJSONArray("descriptions"));
            appendAll(mergedAssetProperties, pagePayload.getJSONArray("asset_properties"));

            boolean moreItems = pagePayload.getBooleanValue("more_items");
            String nextAssetId = pagePayload.getString("last_assetid");
            page++;

            if (!moreItems || nextAssetId == null || nextAssetId.isBlank() || nextAssetId.equals(lastAssetId)) {
                mergedPayload.put("assets", mergedAssets);
                mergedPayload.put("descriptions", mergedDescriptions);
                mergedPayload.put("asset_properties", mergedAssetProperties);
                mergedPayload.put("more_items", false);
                return mergedPayload;
            }

            lastAssetId = nextAssetId;
        }

        if (mergedPayload != null) {
            mergedPayload.put("assets", mergedAssets);
            mergedPayload.put("descriptions", mergedDescriptions);
            mergedPayload.put("asset_properties", mergedAssetProperties);
            mergedPayload.put("more_items", true);
            return mergedPayload;
        }

        return null;
    }

    private JSONObject requestInventoryPage(String url) {
        int maxRetries = 3;
        Exception lastException = null;

        for (int retryCount = 1; retryCount <= maxRetries; retryCount++) {
            try {
                Request.Builder requestBuilder = new Request.Builder()
                        .url(url)
                        .get()
                        .header("User-Agent", defaultUserAgent())
                        .header("Accept", "application/json, text/plain, */*")
                        .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                        .header("Cache-Control", "no-cache");

                if (steamLoginSecure != null && !steamLoginSecure.isBlank()) {
                    requestBuilder.header("Cookie", "steamLoginSecure=" + steamLoginSecure);
                }

                try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
                    int responseCode = response.code();
                    String responseBody = response.body() != null ? response.body().string() : "";

                    log.info("Steam inventory response: status={}, bodyLength={}", responseCode, responseBody.length());

                    if (responseCode == 200) {
                        return JSON.parseObject(responseBody);
                    }

                    if (responseCode == 429) {
                        throw new IllegalStateException("Steam inventory rate limited");
                    }

                    log.error("Steam inventory request failed: status={}, body={}", responseCode, preview(responseBody, 500));
                    return null;
                }
            } catch (Exception e) {
                lastException = e;
                log.warn("Steam inventory request retry {}/{} failed: {}", retryCount, maxRetries, e.getMessage());

                if (retryCount >= maxRetries) {
                    break;
                }

                try {
                    Thread.sleep(Duration.ofSeconds(retryCount).toMillis());
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (lastException != null) {
            log.error("Steam inventory request failed after retries. proxyEnabled={}, proxyType={}, proxyHost={}, proxyPort={}",
                    proxyEnabled, proxyType, proxyHost, proxyPort, lastException);
            throw new RuntimeException("Failed to connect to Steam inventory API: " + lastException.getMessage(), lastException);
        }

        return null;
    }

    private void appendAll(JSONArray target, JSONArray source) {
        if (target == null || source == null || source.isEmpty()) {
            return;
        }

        for (int index = 0; index < source.size(); index++) {
            target.add(source.get(index));
        }
    }

    public SteamMarketPageResult getCsgoItems(int start, int count) {
        String url = String.format(
                "https://steamcommunity.com/market/search/render/?query=&start=%d&count=%d&search_descriptions=1&sort_column=name&sort_dir=asc&appid=730&norender=1&currency=23&country=CN&l=schinese",
                start,
                count
        );
        log.info("Request Steam market page: start={}, count={}, proxyEnabled={}, proxyType={}, proxyHost={}, proxyPort={}",
                start, count, proxyEnabled, proxyType, proxyHost, proxyPort);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", defaultUserAgent())
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", "https://steamcommunity.com/market/")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            int statusCode = response.code();
            ResponseBody body = response.body();
            String responseBody = body != null ? body.string() : "";

            log.info("Steam market response: status={}, preview={}", statusCode, preview(responseBody, 300));

            if (statusCode == 200) {
                JSONObject payload = parseMarketSearchJson(responseBody);
                if (payload != null && payload.getBooleanValue("success")) {
                    return SteamMarketPageResult.success(statusCode, payload);
                }
                return SteamMarketPageResult.failure(statusCode, payload != null ? payload.getString("error") : "Steam market response parsing failed");
            }

            if (statusCode == 429) {
                return SteamMarketPageResult.rateLimited(statusCode, parseRetryAfterSeconds(response.header("Retry-After")), "Steam market rate limited");
            }

            return SteamMarketPageResult.failure(statusCode, "Steam market request failed with HTTP " + statusCode);
        } catch (Exception e) {
            log.error("Steam market request failed: start={}, count={}, message={}", start, count, e.getMessage(), e);
            return SteamMarketPageResult.failure(0, e.getMessage());
        }
    }

    public JSONObject getMarketPriceOverview(String marketHashName) {
        if (marketHashName == null || marketHashName.isBlank()) {
            return null;
        }

        String encodedName = URLEncoder.encode(marketHashName, StandardCharsets.UTF_8);
        String url = "https://steamcommunity.com/market/priceoverview/?appid=730&currency=23&country=CN&l=schinese&market_hash_name="
                + encodedName;
        log.info("Request Steam market price overview: marketHashName={}", marketHashName);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", defaultUserAgent())
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Referer", "https://steamcommunity.com/market/")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            int statusCode = response.code();
            ResponseBody body = response.body();
            String responseBody = body != null ? body.string() : "";
            log.info("Steam market price overview response: status={}, preview={}", statusCode, preview(responseBody, 300));

            if (statusCode == 200) {
                return JSON.parseObject(responseBody);
            }

            return null;
        } catch (Exception e) {
            log.warn("Steam market price overview request failed: marketHashName={}, message={}", marketHashName, e.getMessage());
            return null;
        }
    }

    public JSONObject getMarketPriceHistory(String marketHashName) {
        if (marketHashName == null || marketHashName.isBlank()) {
            return null;
        }

        String encodedName = URLEncoder.encode(marketHashName, StandardCharsets.UTF_8);
        String url = "https://steamcommunity.com/market/pricehistory/?appid=730&currency=23&country=CN&l=english&market_hash_name="
                + encodedName;
        log.info("Request Steam market price history: marketHashName={}", marketHashName);

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", defaultUserAgent())
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Referer", "https://steamcommunity.com/market/");

        if (steamLoginSecure != null && !steamLoginSecure.isBlank()) {
            requestBuilder.header("Cookie", "steamLoginSecure=" + steamLoginSecure);
        }

        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            int statusCode = response.code();
            ResponseBody body = response.body();
            String responseBody = body != null ? body.string() : "";
            log.info("Steam market price history response: status={}, preview={}", statusCode, preview(responseBody, 300));

            if (statusCode == 200) {
                JSONObject payload = JSON.parseObject(responseBody);
                if (payload != null && payload.getBooleanValue("success") && payload.getJSONArray("prices") != null) {
                    return payload;
                }
            }

            return getMarketPriceHistoryFromListingPage(marketHashName);
        } catch (Exception e) {
            log.warn("Steam market price history request failed: marketHashName={}, message={}", marketHashName, e.getMessage());
            return getMarketPriceHistoryFromListingPage(marketHashName);
        }
    }

    private JSONObject getMarketPriceHistoryFromListingPage(String marketHashName) {
        String encodedName = URLEncoder.encode(marketHashName, StandardCharsets.UTF_8).replace("+", "%20");
        String url = "https://steamcommunity.com/market/listings/730/" + encodedName + "?currency=23&country=CN&l=english";
        log.info("Fallback request Steam market listing page for price history: marketHashName={}", marketHashName);

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", defaultUserAgent())
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Referer", "https://steamcommunity.com/market/");

        if (steamLoginSecure != null && !steamLoginSecure.isBlank()) {
            requestBuilder.header("Cookie", "steamLoginSecure=" + steamLoginSecure);
        }

        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            ResponseBody body = response.body();
            String responseBody = body != null ? body.string() : "";
            Matcher matcher = STEAM_LISTING_HISTORY_PATTERN.matcher(responseBody);
            if (!matcher.find()) {
                return null;
            }

            JSONObject payload = new JSONObject();
            payload.put("success", true);
            payload.put("prices", JSON.parseArray(matcher.group(1)));
            return payload;
        } catch (Exception e) {
            log.warn("Steam market listing page price history fallback failed: marketHashName={}, message={}",
                    marketHashName, e.getMessage());
            return null;
        }
    }

    private JSONObject parseMarketSearchJson(String json) {
        try {
            JSONObject jsonResponse = JSON.parseObject(json);
            if (!jsonResponse.getBooleanValue("success")) {
                JSONObject error = new JSONObject();
                error.put("success", false);
                error.put("error", jsonResponse.getString("message"));
                return error;
            }

            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("results", jsonResponse.getJSONArray("results"));
            result.put("count", jsonResponse.getJSONArray("results") != null ? jsonResponse.getJSONArray("results").size() : 0);
            result.put("total_count", jsonResponse.getIntValue("total_count", 0));
            return result;
        } catch (Exception e) {
            log.error("Failed to parse Steam market JSON", e);
            JSONObject error = new JSONObject();
            error.put("success", false);
            error.put("error", e.getMessage());
            return error;
        }
    }

    private Integer parseRetryAfterSeconds(String retryAfterHeader) {
        if (retryAfterHeader == null || retryAfterHeader.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(retryAfterHeader.trim());
        } catch (NumberFormatException e) {
            log.warn("Unable to parse Retry-After header: {}", retryAfterHeader);
            return null;
        }
    }

    private String preview(String text, int maxLength) {
        if (text == null || text.isEmpty()) {
            return "<empty>";
        }
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    private String defaultUserAgent() {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";
    }
}
