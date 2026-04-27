package com.cs2trade.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONArray;
import com.cs2trade.entity.Item;
import com.cs2trade.entity.User;
import com.cs2trade.entity.UserInventory;
import com.cs2trade.mapper.ItemMapper;
import com.cs2trade.mapper.UserInventoryMapper;
import com.cs2trade.mapper.UserMapper;
import com.cs2trade.service.InspectMetadataService;
import com.cs2trade.util.SteamApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SteamInventoryServiceImplTest {

    @Mock
    private UserInventoryMapper userInventoryMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ItemMapper itemMapper;

    @Mock
    private SteamApiClient steamApiClient;

    @Mock
    private InspectMetadataService inspectMetadataService;

    private SteamInventoryServiceImpl steamInventoryService;

    @BeforeEach
    void setUp() {
        steamInventoryService = new SteamInventoryServiceImpl(
                userInventoryMapper,
                userMapper,
                itemMapper,
                steamApiClient,
                inspectMetadataService
        );
    }

    @Test
    void syncInventoryReusesFetchedSteamSnapshotForResponse() {
        User user = new User();
        user.setId(1L);
        user.setSteamId("steam-64");

        when(userMapper.selectById(1L)).thenReturn(user);
        when(userInventoryMapper.selectByUserId(1L)).thenReturn(List.of());
        when(userInventoryMapper.deleteUnreferencedByUserId(1L)).thenReturn(0);
        when(steamApiClient.getInventory("steam-64")).thenReturn(new JSONObject());

        assertTrue(steamInventoryService.syncInventory(1L).isEmpty());

        verify(steamApiClient, times(1)).getInventory("steam-64");
        verify(userInventoryMapper, times(2)).selectByUserId(1L);
        verify(userInventoryMapper).deleteUnreferencedByUserId(1L);
        verifyNoInteractions(itemMapper, inspectMetadataService);
    }

    @Test
    void syncInventoryMatchesItemsByMarketHashName() {
        User user = new User();
        user.setId(1L);
        user.setSteamId("steam-64");

        Item item = new Item();
        item.setId(100L);
        item.setItemId("Souvenir Sawed-Off | Parched (Field-Tested)");
        item.setName("Souvenir Sawed-Off | Parched (Field-Tested)");
        item.setSteamReferencePrice(new BigDecimal("4.18"));

        JSONObject asset = new JSONObject();
        asset.put("assetid", "asset-1");
        asset.put("classid", "class-1");
        asset.put("instanceid", "instance-1");

        JSONObject description = new JSONObject();
        description.put("classid", "class-1");
        description.put("instanceid", "instance-1");
        description.put("market_hash_name", "Souvenir Sawed-Off | Parched (Field-Tested)");
        description.put("name", "截短霰弹枪（纪念品） | 旱地之花");
        description.put("icon_url", "icon.png");
        description.put("tradable", 1);
        description.put("marketable", 1);

        JSONArray assets = new JSONArray();
        assets.add(asset);
        JSONArray descriptions = new JSONArray();
        descriptions.add(description);

        JSONObject payload = new JSONObject();
        payload.put("assets", assets);
        payload.put("descriptions", descriptions);

        when(userMapper.selectById(1L)).thenReturn(user);
        when(steamApiClient.getInventory("steam-64")).thenReturn(payload);
        when(itemMapper.selectAllActive()).thenReturn(List.of(item));
        when(userInventoryMapper.selectByUserId(1L)).thenReturn(List.of(), List.of());
        when(userInventoryMapper.deleteUnreferencedByUserId(1L)).thenReturn(0);

        steamInventoryService.syncInventory(1L);

        ArgumentCaptor<UserInventory> inventoryCaptor = ArgumentCaptor.forClass(UserInventory.class);
        verify(userInventoryMapper).insert(inventoryCaptor.capture());
        UserInventory inserted = inventoryCaptor.getValue();
        assertEquals(100L, inserted.getItemId());
        assertEquals("Souvenir Sawed-Off | Parched (Field-Tested)", inserted.getMarketHashName());
        assertEquals(new BigDecimal("4.18"), inserted.getMarketPrice());
    }

    @Test
    void syncInventoryBackfillsFallbackItemIconAndHashName() {
        User user = baseUser();

        Item item = new Item();
        item.setId(1750L);
        item.setItemId("StatTrak? CZ75-Auto | Yellow Jacket (Minimal Wear)");
        item.setName("StatTrak? CZ75-Auto | Yellow Jacket (Minimal Wear)");
        item.setIconUrl("https://cdn.akamai.steamstatic.com/steamcommunity/public/images/apps/730/app-icon.jpg");
        item.setSteamReferencePrice(new BigDecimal("23.84"));

        JSONObject payload = inventoryPayload(
                "asset-1",
                "StatTrak™ CZ75-Auto | Yellow Jacket (Minimal Wear)",
                "StatTrak™ CZ75-Auto | Yellow Jacket (Minimal Wear)",
                "small-icon",
                "large-icon"
        );

        when(userMapper.selectById(1L)).thenReturn(user);
        when(steamApiClient.getInventory("steam-64")).thenReturn(payload);
        when(itemMapper.selectAllActive()).thenReturn(List.of(item));
        when(userInventoryMapper.selectByUserId(1L)).thenReturn(List.of(), List.of());
        when(userInventoryMapper.deleteUnreferencedByUserId(1L)).thenReturn(0);

        steamInventoryService.syncInventory(1L);

        ArgumentCaptor<Item> itemCaptor = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper).updateById(itemCaptor.capture());
        Item updated = itemCaptor.getValue();
        assertEquals(1750L, updated.getId());
        assertEquals("StatTrak™ CZ75-Auto | Yellow Jacket (Minimal Wear)", updated.getSteamMarketHashName());
        assertEquals(
                "https://steamcommunity.com/market/listings/730/StatTrak%E2%84%A2%20CZ75-Auto%20%7C%20Yellow%20Jacket%20%28Minimal%20Wear%29",
                updated.getSteamMarketUrl()
        );
        assertEquals("https://steamcommunity-a.akamaihd.net/economy/image/large-icon", updated.getIconUrl());

        ArgumentCaptor<UserInventory> inventoryCaptor = ArgumentCaptor.forClass(UserInventory.class);
        verify(userInventoryMapper).insert(inventoryCaptor.capture());
        assertEquals(1750L, inventoryCaptor.getValue().getItemId());
    }

    @Test
    void syncInventoryDoesNotOverwriteExistingRealItemIcon() {
        User user = baseUser();

        Item item = new Item();
        item.setId(5271L);
        item.setItemId("P250 | Kintsugi (Minimal Wear)");
        item.setName("P250 | Kintsugi (Minimal Wear)");
        item.setSteamMarketHashName("P250 | Kintsugi (Minimal Wear)");
        item.setSteamMarketUrl("https://steamcommunity.com/market/listings/730/P250%20%7C%20Kintsugi%20%28Minimal%20Wear%29");
        item.setIconUrl("https://steamcommunity-a.akamaihd.net/economy/image/existing-real-icon");
        item.setSteamReferencePrice(new BigDecimal("23.98"));

        JSONObject payload = inventoryPayload(
                "asset-1",
                "P250 | Kintsugi (Minimal Wear)",
                "P250 | Kintsugi (Minimal Wear)",
                "small-icon",
                "large-icon"
        );

        when(userMapper.selectById(1L)).thenReturn(user);
        when(steamApiClient.getInventory("steam-64")).thenReturn(payload);
        when(itemMapper.selectAllActive()).thenReturn(List.of(item));
        when(userInventoryMapper.selectByUserId(1L)).thenReturn(List.of(), List.of());
        when(userInventoryMapper.deleteUnreferencedByUserId(1L)).thenReturn(0);

        steamInventoryService.syncInventory(1L);

        verify(itemMapper, never()).updateById(any(Item.class));

        ArgumentCaptor<UserInventory> inventoryCaptor = ArgumentCaptor.forClass(UserInventory.class);
        verify(userInventoryMapper).insert(inventoryCaptor.capture());
        assertEquals(5271L, inventoryCaptor.getValue().getItemId());
    }

    @Test
    void syncInventoryFallsBackToUnknownItemWhenInventoryBackedItemCreationFails() {
        User user = new User();
        user.setId(1L);
        user.setSteamId("steam-64");

        Item unknownItem = new Item();
        unknownItem.setId(999L);
        unknownItem.setItemId("unknown");
        unknownItem.setName("Unknown Item");

        JSONObject asset = new JSONObject();
        asset.put("assetid", "asset-1");
        asset.put("classid", "class-1");
        asset.put("instanceid", "instance-1");

        JSONObject description = new JSONObject();
        description.put("classid", "class-1");
        description.put("instanceid", "instance-1");
        description.put("market_hash_name", "AK-47 | Head Shot (Field-Tested)");
        description.put("name", "AK-47 | Head Shot");
        description.put("icon_url", "icon.png");
        description.put("tradable", 1);
        description.put("marketable", 1);

        JSONArray assets = new JSONArray();
        assets.add(asset);
        JSONArray descriptions = new JSONArray();
        descriptions.add(description);

        JSONObject payload = new JSONObject();
        payload.put("assets", assets);
        payload.put("descriptions", descriptions);

        when(userMapper.selectById(1L)).thenReturn(user);
        when(steamApiClient.getInventory("steam-64")).thenReturn(payload);
        when(itemMapper.selectAllActive()).thenReturn(List.of(), List.of(unknownItem));
        when(itemMapper.selectByItemId("AK-47 | Head Shot (Field-Tested)")).thenReturn(null);
        when(userInventoryMapper.selectByUserId(1L)).thenReturn(List.of(), List.of());
        when(userInventoryMapper.deleteUnreferencedByUserId(1L)).thenReturn(0);
        doAnswer(invocation -> {
            Item item = invocation.getArgument(0);
            if (!"unknown".equals(item.getItemId())) {
                throw new RuntimeException("Data truncation");
            }
            return 1;
        }).when(itemMapper).insert(any(Item.class));

        List<UserInventory> synced = steamInventoryService.syncInventory(1L);

        ArgumentCaptor<UserInventory> inventoryCaptor = ArgumentCaptor.forClass(UserInventory.class);
        verify(userInventoryMapper).insert(inventoryCaptor.capture());
        UserInventory inserted = inventoryCaptor.getValue();
        assertEquals(999L, inserted.getItemId());
        assertEquals(1, synced.size());
        assertEquals(999L, synced.get(0).getItemId());
    }

    private User baseUser() {
        User user = new User();
        user.setId(1L);
        user.setSteamId("steam-64");
        return user;
    }

    private JSONObject inventoryPayload(String assetId,
                                        String marketHashName,
                                        String displayName,
                                        String iconUrl,
                                        String iconUrlLarge) {
        JSONObject asset = new JSONObject();
        asset.put("assetid", assetId);
        asset.put("classid", "class-1");
        asset.put("instanceid", "instance-1");

        JSONObject description = new JSONObject();
        description.put("classid", "class-1");
        description.put("instanceid", "instance-1");
        description.put("market_hash_name", marketHashName);
        description.put("name", displayName);
        description.put("icon_url", iconUrl);
        description.put("icon_url_large", iconUrlLarge);
        description.put("tradable", 1);
        description.put("marketable", 1);

        JSONArray assets = new JSONArray();
        assets.add(asset);
        JSONArray descriptions = new JSONArray();
        descriptions.add(description);

        JSONObject payload = new JSONObject();
        payload.put("assets", assets);
        payload.put("descriptions", descriptions);
        return payload;
    }
}
