package com.cs2trade.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.cs2trade.config.SteamMarketSyncProperties;
import com.cs2trade.entity.Item;
import com.cs2trade.mapper.ItemMapper;
import com.cs2trade.mapper.SteamSyncTaskMapper;
import com.cs2trade.util.SteamApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void detectsSteamMarketHashNameChanges() {
        Item existing = baseItem();
        existing.setSteamMarketHashName("Old Hash Name");

        Item latest = baseItem();
        latest.setSteamMarketHashName("New Hash Name");

        assertTrue(itemService.needsUpdate(existing, latest));
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
        item.setName("Sawed-Off | Dry Season (Field-Tested)");
        item.setNameCn("Sawed-Off | Dry Season (Field-Tested)");
        item.setIconUrl("icon-url");
        item.setCategory("shotgun");
        return item;
    }
}
