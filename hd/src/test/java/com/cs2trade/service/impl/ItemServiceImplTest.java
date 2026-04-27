package com.cs2trade.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONArray;
import com.cs2trade.config.SteamMarketSyncProperties;
import com.cs2trade.entity.Item;
import com.cs2trade.mapper.ItemMapper;
import com.cs2trade.mapper.SteamSyncTaskMapper;
import com.cs2trade.util.SteamApiClient;
import com.cs2trade.util.SteamMarketPageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemMapper itemMapper;

    @Mock
    private SteamApiClient steamApiClient;

    @Mock
    private SteamSyncTaskMapper steamSyncTaskMapper;

    private ItemServiceImpl itemService;

    @BeforeEach
    void setUp() {
        itemService = new ItemServiceImpl(
                itemMapper,
                steamApiClient,
                steamSyncTaskMapper,
                new SteamMarketSyncProperties()
        );
    }

    @Test
    void parsesSellPriceAsSteamReferencePrice() {
        JSONObject payload = baseSteamItem();
        payload.put("sell_price", 990);

        Item item = itemService.parseSteamItem(payload);

        assertEquals("Sawed-Off | Dry Season (Field-Tested)", item.getSteamMarketHashName());
        assertEquals("https://steamcommunity.com/market/listings/730/Sawed-Off%20%7C%20Dry%20Season%20%28Field-Tested%29",
                item.getSteamMarketUrl());
        assertEquals(new BigDecimal("9.90"), item.getSteamReferencePrice());
        assertEquals("CNY", item.getSteamReferenceCurrency());
        assertEquals("steam_market_search", item.getSteamReferencePriceSource());
        assertNull(item.getBuffPrice());
    }

    @Test
    void parsesPriceValueFallbackAsSteamReferencePrice() {
        JSONObject payload = baseSteamItem();
        payload.put("price_value", 5500);

        Item item = itemService.parseSteamItem(payload);

        assertEquals(new BigDecimal("55.00"), item.getSteamReferencePrice());
        assertNull(item.getBuffPrice());
    }

    @Test
    void leavesSteamReferencePriceNullWhenSteamPriceIsMissing() {
        Item item = itemService.parseSteamItem(baseSteamItem());

        assertNull(item.getSteamReferencePrice());
        assertNull(item.getBuffPrice());
    }

    @Test
    void parsesNestedAssetDescriptionHashAndIcon() {
        JSONObject assetDescription = new JSONObject();
        assetDescription.put("market_hash_name", "P250 | Kintsugi (Minimal Wear)");
        assetDescription.put("icon_url", "nested-icon");

        JSONObject payload = new JSONObject();
        payload.put("name", "P250 | Kintsugi (Minimal Wear)");
        payload.put("hash_name", "P250 | Kintsugi (Minimal Wear)");
        payload.put("app_icon", "https://cdn.fastly.steamstatic.com/steamcommunity/public/images/apps/730/app-icon.jpg");
        payload.put("asset_description", assetDescription);

        Item item = itemService.parseSteamItem(payload);

        assertEquals("P250 | Kintsugi (Minimal Wear)", item.getItemId());
        assertEquals("P250 | Kintsugi (Minimal Wear)", item.getSteamMarketHashName());
        assertEquals("https://steamcommunity-a.akamaihd.net/economy/image/nested-icon", item.getIconUrl());
    }

    @Test
    void parsesQualityAndCategoryFromAssetDescriptionType() {
        JSONObject assetDescription = new JSONObject();
        assetDescription.put("market_hash_name", "AWP | Neo-Noir (Factory New)");
        assetDescription.put("icon_url", "awp-icon");
        assetDescription.put("type", "保密级 狙击步枪");
        assetDescription.put("name_color", "d32ce6");

        JSONObject payload = new JSONObject();
        payload.put("name", "AWP | 黑色魅影 (崭新出厂)");
        payload.put("hash_name", "AWP | Neo-Noir (Factory New)");
        payload.put("asset_description", assetDescription);

        Item item = itemService.parseSteamItem(payload);

        assertEquals("sniper_rifle", item.getCategory());
        assertEquals("classified", item.getQuality());
        assertEquals("classified", item.getRarity());
    }

    @Test
    void preservesSpecialCategoriesWhenAssetTypeIsMissing() {
        JSONObject casePayload = new JSONObject();
        casePayload.put("name", "Revolution Case");
        casePayload.put("market_hash_name", "Revolution Case");

        JSONObject musicPayload = new JSONObject();
        musicPayload.put("name", "Music Kit | Scarlxrd, CHAIN$AW.LXADXUT.");
        musicPayload.put("market_hash_name", "Music Kit | Scarlxrd, CHAIN$AW.LXADXUT.");

        JSONObject graffitiPayload = new JSONObject();
        graffitiPayload.put("name", "Sealed Graffiti | Recoil M4A4 (Bazooka Pink)");
        graffitiPayload.put("market_hash_name", "Sealed Graffiti | Recoil M4A4 (Bazooka Pink)");

        assertEquals("case", itemService.parseSteamItem(casePayload).getCategory());
        assertEquals("music", itemService.parseSteamItem(musicPayload).getCategory());
        assertEquals("graffiti", itemService.parseSteamItem(graffitiPayload).getCategory());
    }

    @Test
    void detectsSteamReferencePriceChanges() {
        Item existing = baseItem();
        existing.setSteamReferencePrice(new BigDecimal("9.90"));
        existing.setSteamReferenceCurrency("CNY");
        existing.setSteamReferencePriceSource("legacy_buff_price");

        Item latest = baseItem();
        latest.setSteamReferencePrice(new BigDecimal("10.50"));
        latest.setSteamReferenceCurrency("CNY");
        latest.setSteamReferencePriceSource("steam_market_search");

        assertTrue(itemService.needsUpdate(existing, latest));
    }

    @Test
    void detectsMetadataChangesWhenQualityChanges() {
        Item existing = baseItem();
        existing.setQuality("common");
        existing.setRarity("common");

        Item latest = baseItem();
        latest.setQuality("covert");
        latest.setRarity("covert");

        assertTrue(itemService.needsUpdate(existing, latest));
    }

    @Test
    void detectsSteamMarketHashNameChanges() {
        Item existing = baseItem();
        existing.setSteamMarketHashName("Old Hash Name");
        existing.setSteamMarketUrl("https://steamcommunity.com/market/listings/730/Old%20Hash%20Name");

        Item latest = baseItem();
        latest.setSteamMarketHashName("New Hash Name");
        latest.setSteamMarketUrl("https://steamcommunity.com/market/listings/730/New%20Hash%20Name");

        assertTrue(itemService.needsUpdate(existing, latest));
    }

    @Test
    void syncItemsFromSteamUpdatesLegacyStatTrakItemInPlace() {
        Item legacyItem = new Item();
        legacyItem.setId(1750L);
        legacyItem.setItemId("StatTrak? CZ75-Auto | Yellow Jacket (Minimal Wear)");
        legacyItem.setName("StatTrak? CZ75-Auto | Yellow Jacket (Minimal Wear)");
        legacyItem.setNameCn("StatTrak? CZ75-Auto | Yellow Jacket (Minimal Wear)");
        legacyItem.setCategory("pistol");
        legacyItem.setIconUrl("https://cdn.akamai.steamstatic.com/steamcommunity/public/images/apps/730/app-icon.jpg");
        legacyItem.setCreatedAt(LocalDateTime.of(2025, 1, 1, 0, 0));

        JSONObject steamItem = new JSONObject();
        steamItem.put("name", "StatTrak™ CZ75-Auto | Yellow Jacket (Minimal Wear)");
        steamItem.put("hash_name", "StatTrak™ CZ75-Auto | Yellow Jacket (Minimal Wear)");
        steamItem.put("app_icon", "https://cdn.fastly.steamstatic.com/steamcommunity/public/images/apps/730/app-icon.jpg");
        steamItem.put("sell_price", 2418);
        JSONObject assetDescription = new JSONObject();
        assetDescription.put("market_hash_name", "StatTrak™ CZ75-Auto | Yellow Jacket (Minimal Wear)");
        assetDescription.put("icon_url", "real-icon");
        steamItem.put("asset_description", assetDescription);

        JSONArray results = new JSONArray();
        results.add(steamItem);

        JSONObject payload = new JSONObject();
        payload.put("total_count", 1);
        payload.put("results", results);

        when(steamSyncTaskMapper.selectLatestByTaskType(anyString())).thenReturn(null);
        when(itemMapper.selectAllActive()).thenReturn(List.of(legacyItem));
        when(steamApiClient.getCsgoItems(0, 10)).thenReturn(SteamMarketPageResult.success(200, payload));

        int changedCount = itemService.syncItemsFromSteam();

        assertEquals(1, changedCount);

        ArgumentCaptor<Item> itemCaptor = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper).updateById(itemCaptor.capture());
        verify(itemMapper, never()).insert(any(Item.class));

        Item updated = itemCaptor.getValue();
        assertEquals(1750L, updated.getId());
        assertEquals("StatTrak™ CZ75-Auto | Yellow Jacket (Minimal Wear)", updated.getItemId());
        assertEquals("StatTrak™ CZ75-Auto | Yellow Jacket (Minimal Wear)", updated.getSteamMarketHashName());
        assertEquals(
                "https://steamcommunity.com/market/listings/730/StatTrak%E2%84%A2%20CZ75-Auto%20%7C%20Yellow%20Jacket%20%28Minimal%20Wear%29",
                updated.getSteamMarketUrl()
        );
        assertEquals("https://steamcommunity-a.akamaihd.net/economy/image/real-icon", updated.getIconUrl());
    }

    @Test
    void syncItemsFromSteamBackfillsLegacyQualityMetadata() {
        Item legacyItem = new Item();
        legacyItem.setId(2912L);
        legacyItem.setItemId("M4A4 | Temukau (Field-Tested)");
        legacyItem.setName("M4A4 | 反冲精英 (久经沙场)");
        legacyItem.setNameCn("M4A4 | 反冲精英 (久经沙场)");
        legacyItem.setCategory("rifle");
        legacyItem.setQuality("common");
        legacyItem.setRarity("common");
        legacyItem.setIconUrl("https://steamcommunity-a.akamaihd.net/economy/image/old-icon");
        legacyItem.setSteamMarketHashName("M4A4 | Temukau (Field-Tested)");
        legacyItem.setSteamMarketUrl("https://steamcommunity.com/market/listings/730/M4A4%20%7C%20Temukau%20%28Field-Tested%29");
        legacyItem.setCreatedAt(LocalDateTime.of(2026, 3, 29, 15, 29, 18));

        JSONObject steamItem = new JSONObject();
        steamItem.put("name", "M4A4 | 反冲精英 (久经沙场)");
        steamItem.put("hash_name", "M4A4 | Temukau (Field-Tested)");
        steamItem.put("sell_price", 5003);
        JSONObject assetDescription = new JSONObject();
        assetDescription.put("market_hash_name", "M4A4 | Temukau (Field-Tested)");
        assetDescription.put("icon_url", "temukau-icon");
        assetDescription.put("type", "隐秘级 步枪");
        assetDescription.put("name_color", "eb4b4b");
        steamItem.put("asset_description", assetDescription);

        JSONArray results = new JSONArray();
        results.add(steamItem);

        JSONObject payload = new JSONObject();
        payload.put("total_count", 1);
        payload.put("results", results);

        when(steamSyncTaskMapper.selectLatestByTaskType(anyString())).thenReturn(null);
        when(itemMapper.selectAllActive()).thenReturn(List.of(legacyItem));
        when(steamApiClient.getCsgoItems(0, 10)).thenReturn(SteamMarketPageResult.success(200, payload));

        int changedCount = itemService.syncItemsFromSteam();

        assertEquals(1, changedCount);

        ArgumentCaptor<Item> itemCaptor = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper).updateById(itemCaptor.capture());
        verify(itemMapper, never()).insert(any(Item.class));

        Item updated = itemCaptor.getValue();
        assertEquals(2912L, updated.getId());
        assertEquals("rifle", updated.getCategory());
        assertEquals("covert", updated.getQuality());
        assertEquals("covert", updated.getRarity());
    }

    @Test
    void syncItemsFromSteamRepairsChineseLegacyDuplicateMetadata() {
        Item canonicalItem = new Item();
        canonicalItem.setId(15181L);
        canonicalItem.setItemId("Sticker | Ninjas in Pyjamas (Foil) | Katowice 2019");
        canonicalItem.setName("印花 | Ninjas in Pyjamas（闪亮）| 2019年卡托维兹锦标赛");
        canonicalItem.setNameCn("印花 | Ninjas in Pyjamas（闪亮）| 2019年卡托维兹锦标赛");
        canonicalItem.setCategory("other");
        canonicalItem.setQuality("consumer");
        canonicalItem.setRarity("consumer");
        canonicalItem.setIconUrl("https://cdn.akamai.steamstatic.com/steamcommunity/public/images/apps/730/app-icon.jpg");
        canonicalItem.setSteamReferencePrice(new BigDecimal("20.24"));
        canonicalItem.setSteamReferenceCurrency("CNY");
        canonicalItem.setSteamReferencePriceSource("legacy_buff_price");
        canonicalItem.setCreatedAt(LocalDateTime.of(2025, 1, 1, 0, 0));

        Item legacyChineseItem = new Item();
        legacyChineseItem.setId(20296L);
        legacyChineseItem.setItemId("印花 | Ninjas in Pyjamas（闪亮）| 2019年卡托维兹锦标赛");
        legacyChineseItem.setName("印花 | Ninjas in Pyjamas（闪亮）| 2019年卡托维兹锦标赛");
        legacyChineseItem.setNameCn("印花 | Ninjas in Pyjamas（闪亮）| 2019年卡托维兹锦标赛");
        legacyChineseItem.setCategory("other");
        legacyChineseItem.setQuality("consumer");
        legacyChineseItem.setRarity("consumer");
        legacyChineseItem.setIconUrl("https://cdn.akamai.steamstatic.com/steamcommunity/public/images/apps/730/app-icon.jpg");
        legacyChineseItem.setCreatedAt(LocalDateTime.of(2026, 4, 22, 21, 23, 25));

        JSONObject steamItem = new JSONObject();
        steamItem.put("name", "印花 | Ninjas in Pyjamas（闪亮）| 2019年卡托维兹锦标赛");
        steamItem.put("hash_name", "Sticker | Ninjas in Pyjamas (Foil) | Katowice 2019");
        steamItem.put("sell_price", 2512);
        steamItem.put("app_icon", "https://cdn.fastly.steamstatic.com/steamcommunity/public/images/apps/730/app-icon.jpg");
        JSONObject assetDescription = new JSONObject();
        assetDescription.put("market_hash_name", "Sticker | Ninjas in Pyjamas (Foil) | Katowice 2019");
        assetDescription.put("icon_url", "real-nip-icon");
        assetDescription.put("type", "High Grade Sticker");
        steamItem.put("asset_description", assetDescription);

        JSONArray results = new JSONArray();
        results.add(steamItem);

        JSONObject payload = new JSONObject();
        payload.put("total_count", 1);
        payload.put("results", results);

        when(steamSyncTaskMapper.selectLatestByTaskType(anyString())).thenReturn(null);
        when(itemMapper.selectAllActive()).thenReturn(List.of(canonicalItem, legacyChineseItem));
        when(steamApiClient.getCsgoItems(0, 10)).thenReturn(SteamMarketPageResult.success(200, payload));

        int changedCount = itemService.syncItemsFromSteam();

        assertEquals(2, changedCount);

        ArgumentCaptor<Item> itemCaptor = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper, never()).insert(any(Item.class));
        verify(itemMapper, org.mockito.Mockito.times(2)).updateById(itemCaptor.capture());

        List<Item> updates = itemCaptor.getAllValues();
        updates.sort(Comparator.comparing(Item::getId));

        Item canonicalUpdate = updates.get(0);
        assertEquals(15181L, canonicalUpdate.getId());
        assertEquals("Sticker | Ninjas in Pyjamas (Foil) | Katowice 2019", canonicalUpdate.getItemId());
        assertEquals("Sticker | Ninjas in Pyjamas (Foil) | Katowice 2019", canonicalUpdate.getSteamMarketHashName());
        assertEquals("sticker", canonicalUpdate.getCategory());
        assertEquals("high-grade", canonicalUpdate.getQuality());
        assertEquals("high-grade", canonicalUpdate.getRarity());
        assertEquals("https://steamcommunity-a.akamaihd.net/economy/image/real-nip-icon", canonicalUpdate.getIconUrl());

        Item duplicateRepair = updates.get(1);
        assertEquals(20296L, duplicateRepair.getId());
        assertNull(duplicateRepair.getItemId());
        assertEquals("Sticker | Ninjas in Pyjamas (Foil) | Katowice 2019", duplicateRepair.getSteamMarketHashName());
        assertEquals("sticker", duplicateRepair.getCategory());
        assertEquals("high-grade", duplicateRepair.getQuality());
        assertEquals("high-grade", duplicateRepair.getRarity());
        assertEquals(
                "https://steamcommunity.com/market/listings/730/Sticker%20%7C%20Ninjas%20in%20Pyjamas%20%28Foil%29%20%7C%20Katowice%202019",
                duplicateRepair.getSteamMarketUrl()
        );
        assertEquals("https://steamcommunity-a.akamaihd.net/economy/image/real-nip-icon", duplicateRepair.getIconUrl());
        assertEquals(new BigDecimal("25.12"), duplicateRepair.getSteamReferencePrice());
    }

    @Test
    void normalizesUnsafeListSortParameters() {
        when(itemMapper.selectList(null, null, null, null, "created_at", "DESC", 0, 20))
                .thenReturn(Collections.emptyList());

        itemService.getItemList(null, null, null, null, "created_at;DROP TABLE item", "DESC;--", 1, 20);

        verify(itemMapper).selectList(null, null, null, null, "created_at", "DESC", 0, 20);
    }

    @Test
    void mapsPriceSortAliasToReferencePriceSort() {
        when(itemMapper.selectList(null, null, null, null, "reference_price", "ASC", 0, 20))
                .thenReturn(Collections.emptyList());

        itemService.getItemList(null, null, null, null, "price", "ASC", 1, 20);

        verify(itemMapper).selectList(null, null, null, null, "reference_price", "ASC", 0, 20);
    }

    private JSONObject baseSteamItem() {
        JSONObject payload = new JSONObject();
        payload.put("name", "Sawed-Off | Dry Season (Field-Tested)");
        payload.put("market_hash_name", "Sawed-Off | Dry Season (Field-Tested)");
        payload.put("icon_url", "icon-url");
        return payload;
    }

    private Item baseItem() {
        Item item = new Item();
        item.setItemId("Sawed-Off | Dry Season (Field-Tested)");
        item.setName("Sawed-Off | Dry Season (Field-Tested)");
        item.setNameCn("Sawed-Off | Dry Season (Field-Tested)");
        item.setIconUrl("icon-url");
        item.setCategory("shotgun");
        item.setSteamMarketHashName("Sawed-Off | Dry Season (Field-Tested)");
        item.setSteamMarketUrl("https://steamcommunity.com/market/listings/730/Sawed-Off%20%7C%20Dry%20Season%20%28Field-Tested%29");
        return item;
    }
}
