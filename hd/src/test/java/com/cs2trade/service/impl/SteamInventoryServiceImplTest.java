package com.cs2trade.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.cs2trade.entity.User;
import com.cs2trade.mapper.ItemMapper;
import com.cs2trade.mapper.UserInventoryMapper;
import com.cs2trade.mapper.UserMapper;
import com.cs2trade.service.InspectMetadataService;
import com.cs2trade.util.SteamApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
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
}
