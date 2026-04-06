package com.cs2trade.util;

import com.alibaba.fastjson2.JSONObject;
import lombok.Getter;

@Getter
public class SteamMarketPageResult {

    private final boolean success;
    private final boolean rateLimited;
    private final int statusCode;
    private final Integer retryAfterSeconds;
    private final JSONObject payload;
    private final String errorMessage;

    private SteamMarketPageResult(boolean success,
                                  boolean rateLimited,
                                  int statusCode,
                                  Integer retryAfterSeconds,
                                  JSONObject payload,
                                  String errorMessage) {
        this.success = success;
        this.rateLimited = rateLimited;
        this.statusCode = statusCode;
        this.retryAfterSeconds = retryAfterSeconds;
        this.payload = payload;
        this.errorMessage = errorMessage;
    }

    public static SteamMarketPageResult success(int statusCode, JSONObject payload) {
        return new SteamMarketPageResult(true, false, statusCode, null, payload, null);
    }

    public static SteamMarketPageResult rateLimited(int statusCode, Integer retryAfterSeconds, String errorMessage) {
        return new SteamMarketPageResult(false, true, statusCode, retryAfterSeconds, null, errorMessage);
    }

    public static SteamMarketPageResult failure(int statusCode, String errorMessage) {
        return new SteamMarketPageResult(false, false, statusCode, null, null, errorMessage);
    }
}
