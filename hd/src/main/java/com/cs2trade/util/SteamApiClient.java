package com.cs2trade.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.net.ssl.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SteamApiClient {

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

    private static final String STEAM_INVENTORY_URL = "https://steamcommunity.com/inventory/%s/730/2";
    private static final String STEAM_CSGO_ITEMS_URL = "https://api.steampowered.com/IEconItems_730/GetSchemaItems/v1/";

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
                            return new X509Certificate[]{};
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true);

            if (proxyEnabled) {
                Proxy.Type type = "SOCKS".equalsIgnoreCase(proxyType) ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
                Proxy proxy = new Proxy(type, new InetSocketAddress(proxyHost, proxyPort));
                builder.proxy(proxy);
                log.info("SteamApiClient已配置代理: {}:{} ({})", proxyHost, proxyPort, proxyType);
            }

            httpClient = builder.build();
            log.info("SteamApiClient初始化完成");
        } catch (Exception e) {
            log.error("SteamApiClient初始化失败: {}", e.getMessage(), e);
            throw new RuntimeException("SteamApiClient初始化失败", e);
        }
    }

    public JSONObject getInventory(String steamId) {
        String url = String.format(STEAM_INVENTORY_URL, steamId) + "?l=schinese&count=2000";
        log.info("请求Steam库存: steamId={}, url={}", steamId, url);

        int maxRetries = 3;
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount < maxRetries) {
            try {
                Request.Builder requestBuilder = new Request.Builder()
                        .url(url)
                        .get()
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                        .header("Accept", "application/json, text/plain, */*")
                        .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                        .header("Cache-Control", "no-cache");

                if (steamLoginSecure != null && !steamLoginSecure.isEmpty()) {
                    requestBuilder.header("Cookie", "steamLoginSecure=" + steamLoginSecure);
                    log.info("已添加Steam登录Cookie");
                }

                Request request = requestBuilder.build();

                try (Response response = httpClient.newCall(request).execute()) {
                    int responseCode = response.code();
                    log.info("Steam API响应码: {}", responseCode);

                    ResponseBody body = response.body();
                    String responseBody = body != null ? body.string() : "";

                    log.info("Steam API响应内容长度: {}", responseBody.length());
                    log.info("Steam API响应内容: {}", responseBody.substring(0, Math.min(500, responseBody.length())));

                    if (responseCode == 200) {
                        return JSON.parseObject(responseBody);
                    }

                    log.error("Steam API返回错误 {}: {}", responseCode, responseBody);

                    if (responseCode == 429) {
                        throw new RuntimeException("Too Many Requests");
                    }

                    return null;
                }

            } catch (Exception e) {
                lastException = e;
                retryCount++;
                log.warn("获取Steam库存失败 (第{}次重试): {}", retryCount, e.getMessage());
                if (retryCount >= maxRetries) break;

                try {
                    Thread.sleep(1000L * retryCount);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (lastException != null) {
            log.error("获取 Steam 库存最终失败：proxyEnabled={}, proxy={}:{}, error={}", proxyEnabled, proxyHost, proxyPort, lastException.getMessage(), lastException);
            throw new RuntimeException("连接 Steam API 失败：" + lastException.getMessage(), lastException);
        }
        return null;
    }

    /**
     * 获取 CS2 所有饰品列表
     * @param start 起始位置（分页偏移）
     * @param count 每页数量（最多 100）
     * @return 包含饰品列表的 JSONObject
     */
    public JSONObject getCsgoItems(int start, int count) {
        // 使用 Steam 社区市场的搜索 API 来获取饰品列表
        // 添加 norender=1 参数获取纯 JSON 数据
        String url = String.format("https://steamcommunity.com/market/search/render/?query=&start=%d&count=%d&search_descriptions=1&sort_column=name&sort_dir=asc&appid=730&norender=1", start, count);
        log.info("请求 Steam 饰品列表：start={}, count={}, url={}", start, count, url);

        try {
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .header("Accept", "application/json, text/javascript, */*; q=0.01")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("Referer", "https://steamcommunity.com/market/")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                int responseCode = response.code();
                log.info("Steam API 响应码：{}", responseCode);

                String responseBody = response.body() != null ? response.body().string() : "";
                
                // 记录响应的前 500 个字符用于调试
                String preview = responseBody.length() > 500 ? responseBody.substring(0, 500) + "..." : responseBody;
                log.info("Steam API 响应内容预览：{}", preview);

                if (responseCode == 200) {
                    // 解析返回的 JSON
                    return parseMarketSearchJson(responseBody);
                }

                log.error("Steam API 返回错误 {}: {}", responseCode, responseBody);
                return null;
            }

        } catch (Exception e) {
            log.error("获取 CS2 饰品列表失败：{}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 解析 Steam 市场搜索 JSON 响应
     */
    private JSONObject parseMarketSearchJson(String json) {
        try {
            JSONObject jsonResponse = JSON.parseObject(json);
            
            if (!jsonResponse.getBooleanValue("success")) {
                log.error("Steam API 返回失败：{}", jsonResponse.getString("message"));
                JSONObject error = new JSONObject();
                error.put("success", false);
                error.put("error", jsonResponse.getString("message"));
                return error;
            }
            
            // 获取总数量
            int totalCount = jsonResponse.getIntValue("total_count", 0);
            log.info("Steam 市场共有 {} 个 CS2 饰品", totalCount);
            
            // 直接解析 results 数组（纯 JSON 格式）
            JSONArray results = jsonResponse.getJSONArray("results");
            if (results == null || results.isEmpty()) {
                log.warn("results 数组为空");
                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("results", new JSONArray());
                result.put("count", 0);
                return result;
            }
            
            log.info("找到 {} 个饰品", results.size());
            
            // 转换数据格式
            JSONArray items = new JSONArray();
            for (int i = 0; i < results.size(); i++) {
                try {
                    JSONObject resultItem = results.getJSONObject(i);
                    
                    JSONObject item = new JSONObject();
                    item.put("market_hash_name", resultItem.getString("hash_name"));
                    item.put("name", resultItem.getString("name"));
                    
                    // 获取应用图标（可以作为临时图片）
                    if (resultItem.containsKey("app_icon")) {
                        item.put("icon_url", resultItem.getString("app_icon"));
                    }
                    
                    // 获取价格信息
                    if (resultItem.containsKey("sell_price")) {
                        item.put("price", resultItem.getString("sell_price_text"));
                        item.put("price_value", resultItem.getInteger("sell_price"));
                    }
                    
                    // 获取出售数量
                    if (resultItem.containsKey("sell_listings")) {
                        item.put("sell_listings", resultItem.getInteger("sell_listings"));
                    }
                    
                    items.add(item);
                } catch (Exception e) {
                    log.warn("解析单个饰品失败：{}", e.getMessage());
                }
            }
            
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("results", items);
            result.put("count", items.size());
            result.put("total_count", totalCount);
            
            log.info("成功解析 {} 个饰品", items.size());
            return result;
            
        } catch (Exception e) {
            log.error("解析 Steam 市场 JSON 失败：{}", e.getMessage(), e);
            JSONObject error = new JSONObject();
            error.put("success", false);
            error.put("error", e.getMessage());
            return error;
        }
    }

    /**
     * 解析 Steam 市场搜索结果
     */
    private JSONObject parseMarketSearchResults(String html) {
        try {
            JSONObject result = new JSONObject();
            
            // 使用 Jsoup 解析 HTML
            Document doc = Jsoup.parse(html);
            
            // 查找包含饰品数据的 script 标签
            // Steam 市场数据通常在 id="searchResultsJSON"的 script 标签中
            Elements scripts = doc.getElementsByTag("script");
            String searchResultsJson = null;
            
            for (Element script : scripts) {
                String scriptData = script.data();
                if (scriptData != null && scriptData.contains("searchResults")) {
                    // 提取 searchResults 变量
                    int startIndex = scriptData.indexOf("var searchResults = ");
                    if (startIndex != -1) {
                        startIndex += "var searchResults = ".length();
                        int endIndex = scriptData.indexOf(";", startIndex);
                        if (endIndex != -1) {
                            searchResultsJson = scriptData.substring(startIndex, endIndex).trim();
                            break;
                        }
                    }
                }
            }
            
            if (searchResultsJson != null) {
                try {
                    JSONObject searchResults = JSON.parseObject(searchResultsJson);
                    JSONArray results = searchResults.getJSONArray("results");
                    
                    if (results != null) {
                        result.put("success", true);
                        result.put("results", results);
                        result.put("count", results.size());
                        log.info("解析到 {} 个饰品", results.size());
                        return result;
                    }
                } catch (Exception e) {
                    log.error("解析 searchResults JSON 失败：{}", e.getMessage());
                }
            }
            
            // 如果找不到 searchResultsJSON，尝试从 HTML 中提取基本信息
            log.info("未找到 searchResultsJSON，尝试从 HTML 中提取饰品信息...");
            
            JSONArray items = new JSONArray();
            
            // 查找市场列表中的物品链接
            Elements itemLinks = doc.select("a.search_result_row");
            
            for (Element link : itemLinks) {
                String hashName = link.attr("data-hash-name");
                String name = link.attr("data-name");
                
                if (hashName != null && !hashName.isEmpty()) {
                    JSONObject item = new JSONObject();
                    item.put("market_hash_name", hashName);
                    item.put("name", name != null ? name : hashName);
                    
                    // 尝试获取图片 URL
                    Element img = link.selectFirst("img");
                    if (img != null) {
                        String imgSrc = img.attr("src");
                        if (imgSrc != null && !imgSrc.isEmpty()) {
                            item.put("icon_url", imgSrc);
                        }
                    }
                    
                    items.add(item);
                }
            }
            
            result.put("success", true);
            result.put("results", items);
            result.put("count", items.size());
            
            log.info("从 HTML 中提取到 {} 个饰品", items.size());
            return result;
            
        } catch (Exception e) {
            log.error("解析 Steam 市场搜索结果失败：{}", e.getMessage(), e);
            JSONObject error = new JSONObject();
            error.put("success", false);
            error.put("error", e.getMessage());
            return error;
        }
    }
}
