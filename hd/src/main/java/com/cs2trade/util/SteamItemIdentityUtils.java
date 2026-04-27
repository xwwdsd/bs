package com.cs2trade.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SteamItemIdentityUtils {

    private static final String STEAM_IMAGE_URL = "https://steamcommunity-a.akamaihd.net/economy/image/";
    private static final String STEAM_APP_ICON_URL_FRAGMENT = "/steamcommunity/public/images/apps/730/";
    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern PIPE_SPACING_PATTERN = Pattern.compile("\\s*\\|\\s*");
    private static final Pattern STAT_TRAK_VARIANT_PATTERN = Pattern.compile("stattrak\\s*(?:tm|[?？])");

    private SteamItemIdentityUtils() {
    }

    public static String normalizeLookupKey(String value) {
        if (value == null) {
            return null;
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replace('\u00A0', ' ')
                .replace('\u2007', ' ')
                .replace('\u202F', ' ')
                .trim()
                .toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return null;
        }

        normalized = PIPE_SPACING_PATTERN.matcher(normalized).replaceAll(" | ");
        normalized = STAT_TRAK_VARIANT_PATTERN.matcher(normalized).replaceAll("stattrak");
        normalized = normalized.replaceAll("\\(\\s+", "(");
        normalized = normalized.replaceAll("\\s+\\)", ")");
        normalized = MULTI_SPACE_PATTERN.matcher(normalized).replaceAll(" ").trim();

        return normalized.isBlank() ? null : normalized;
    }

    public static String normalizeSteamIconUrl(String iconUrl) {
        String normalized = iconUrl == null ? "" : iconUrl.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return normalized;
        }
        if (normalized.startsWith("/")) {
            return normalized;
        }
        return STEAM_IMAGE_URL + normalized;
    }

    public static boolean isUsableItemIconUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return !normalized.equals("default-item.svg")
                && !normalized.equals("default-item.png")
                && !normalized.endsWith("/default-item.svg")
                && !normalized.endsWith("/default-item.png")
                && !normalized.contains(STEAM_APP_ICON_URL_FRAGMENT);
    }

    public static String firstUsableIconUrl(String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            if (isUsableItemIconUrl(value)) {
                return value.trim();
            }
        }
        return null;
    }

    public static String buildSteamMarketUrl(String marketHashName) {
        if (marketHashName == null || marketHashName.isBlank()) {
            return null;
        }

        String encoded = URLEncoder.encode(marketHashName.trim(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return "https://steamcommunity.com/market/listings/730/" + encoded;
    }
}
