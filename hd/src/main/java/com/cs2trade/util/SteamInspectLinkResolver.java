package com.cs2trade.util;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves Steam inventory inspect links that still contain %propid:N% placeholders.
 */
public final class SteamInspectLinkResolver {

    private static final Pattern PROPID_PATTERN = Pattern.compile("%propid:(\\d+)%", Pattern.CASE_INSENSITIVE);

    private SteamInspectLinkResolver() {
    }

    public static String resolve(String link, JSONArray assetProperties) {
        if (link == null || link.isBlank()) {
            return null;
        }

        Matcher matcher = PROPID_PATTERN.matcher(link);
        if (!matcher.find()) {
            return link;
        }

        StringBuffer resolved = new StringBuffer();
        do {
            Integer propertyId = Integer.valueOf(matcher.group(1));
            String propertyValue = findStringProperty(assetProperties, propertyId);
            if (propertyValue == null || propertyValue.isBlank()) {
                return null;
            }
            matcher.appendReplacement(resolved, Matcher.quoteReplacement(propertyValue));
        } while (matcher.find());
        matcher.appendTail(resolved);

        return resolved.toString();
    }

    private static String findStringProperty(JSONArray assetProperties, Integer propertyId) {
        if (assetProperties == null || propertyId == null) {
            return null;
        }

        for (int i = 0; i < assetProperties.size(); i++) {
            JSONObject property = assetProperties.getJSONObject(i);
            if (property == null) {
                continue;
            }

            Integer currentPropertyId = property.getInteger("propertyid");
            if (propertyId.equals(currentPropertyId)) {
                return property.getString("string_value");
            }
        }

        return null;
    }
}
