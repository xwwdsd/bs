package com.cs2trade.util;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SteamInspectLinkResolverTest {

    private static final String PLACEHOLDER_LINK =
            "steam://run/730//+csgo_econ_action_preview%20%propid:6%";

    private static final String CERTIFICATE =
            "6A7A9BA6BCF1D66B726E4A6942695A6E52FFDED88E692AFF68024D1A6E453B792B";

    @Test
    void resolvesInspectCertificatePlaceholder() {
        String resolved = SteamInspectLinkResolver.resolve(PLACEHOLDER_LINK, buildAssetProperties(CERTIFICATE));

        assertEquals(
                "steam://run/730//+csgo_econ_action_preview%206A7A9BA6BCF1D66B726E4A6942695A6E52FFDED88E692AFF68024D1A6E453B792B",
                resolved
        );
    }

    @Test
    void returnsNullWhenRequiredPropertyIsMissing() {
        assertNull(SteamInspectLinkResolver.resolve(PLACEHOLDER_LINK, new JSONArray()));
    }

    @Test
    void resolvedInspectLinkCanBeDecoded() {
        String resolved = SteamInspectLinkResolver.resolve(PLACEHOLDER_LINK, buildAssetProperties(CERTIFICATE));
        Cs2InspectDecoder.DecodedInspectData decoded = new Cs2InspectDecoder().decodeLink(resolved);

        assertEquals(277, decoded.paintSeed());
        assertEquals(0.0171633157D, decoded.paintWear().doubleValue(), 0.0000001D);
        assertNotNull(decoded.paintIndex());
    }

    private JSONArray buildAssetProperties(String certificate) {
        JSONArray assetProperties = new JSONArray();

        JSONObject seedProperty = new JSONObject();
        seedProperty.put("propertyid", 1);
        seedProperty.put("int_value", "277");
        assetProperties.add(seedProperty);

        JSONObject wearProperty = new JSONObject();
        wearProperty.put("propertyid", 2);
        wearProperty.put("float_value", "0.0171633157879114151");
        assetProperties.add(wearProperty);

        JSONObject certificateProperty = new JSONObject();
        certificateProperty.put("propertyid", 6);
        certificateProperty.put("string_value", certificate);
        assetProperties.add(certificateProperty);

        return assetProperties;
    }
}
