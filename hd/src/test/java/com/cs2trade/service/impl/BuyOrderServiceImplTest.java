package com.cs2trade.service.impl;

import com.cs2trade.entity.BuyOrder;
import com.cs2trade.entity.Item;
import com.cs2trade.entity.SellOrder;
import com.cs2trade.entity.TradeOrder;
import com.cs2trade.entity.UserInventory;
import com.cs2trade.mapper.BuyOrderMapper;
import com.cs2trade.mapper.ItemMapper;
import com.cs2trade.mapper.SellOrderMapper;
import com.cs2trade.mapper.UserInventoryMapper;
import com.cs2trade.service.TradeOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.InOrder;

@ExtendWith(MockitoExtension.class)
class BuyOrderServiceImplTest {

    @Mock
    private BuyOrderMapper buyOrderMapper;

    @Mock
    private ItemMapper itemMapper;

    @Mock
    private UserInventoryMapper userInventoryMapper;

    @Mock
    private SellOrderMapper sellOrderMapper;

    @Mock
    private TradeOrderService tradeOrderService;

    private BuyOrderServiceImpl buyOrderService;

    @BeforeEach
    void setUp() {
        buyOrderService = new BuyOrderServiceImpl(
                buyOrderMapper,
                itemMapper,
                userInventoryMapper,
                sellOrderMapper,
                tradeOrderService
        );
    }

    @Test
    void createBuyOrderDefaultsQuantityAndExpiration() {
        Item item = new Item();
        item.setId(10L);
        item.setIsActive(1);
        when(itemMapper.selectById(10L)).thenReturn(item);

        BuyOrder order = buyOrderService.createBuyOrder(1L, 10L, new BigDecimal("8.50"), null, null);

        assertEquals(1L, order.getUserId());
        assertEquals(10L, order.getItemId());
        assertEquals(1, order.getQuantity());
        assertEquals(0, order.getFilledQuantity());
        assertEquals(BuyOrder.STATUS_ACTIVE, order.getStatus());
        assertEquals(BuyOrder.AUTO_ACCEPT_YES, order.getAutoAccept());
        assertNotNull(order.getExpireTime());
        verify(buyOrderMapper).insert(order);
    }

    @Test
    void respondToBuyOrderCreatesLinkedSellOrderAndTradeOrder() {
        BuyOrder buyOrder = new BuyOrder();
        buyOrder.setId(20L);
        buyOrder.setUserId(1L);
        buyOrder.setItemId(10L);
        buyOrder.setPrice(new BigDecimal("8.50"));
        buyOrder.setQuantity(1);
        buyOrder.setFilledQuantity(0);
        buyOrder.setStatus(BuyOrder.STATUS_ACTIVE);
        buyOrder.setExpireTime(LocalDateTime.now().plusDays(1));

        UserInventory inventory = new UserInventory();
        inventory.setId(30L);
        inventory.setUserId(2L);
        inventory.setItemId(10L);
        inventory.setStatus(UserInventory.STATUS_NORMAL);
        inventory.setIsMarketable(UserInventory.IS_MARKETABLE);

        TradeOrder tradeOrder = new TradeOrder();
        tradeOrder.setId(40L);

        when(buyOrderMapper.selectById(20L)).thenReturn(buyOrder);
        when(userInventoryMapper.selectById(30L)).thenReturn(inventory);
        when(sellOrderMapper.insert(any(SellOrder.class))).thenAnswer(invocation -> {
            SellOrder sellOrder = invocation.getArgument(0);
            sellOrder.setId(99L);
            return 1;
        });
        when(buyOrderMapper.incrementFilledQuantity(20L)).thenReturn(1);
        when(tradeOrderService.createOrder(eq(1L), eq(99L), eq(new BigDecimal("8.50")))).thenReturn(tradeOrder);

        TradeOrder result = buyOrderService.respondToBuyOrder(2L, 20L, 30L);

        assertEquals(40L, result.getId());

        ArgumentCaptor<SellOrder> sellOrderCaptor = ArgumentCaptor.forClass(SellOrder.class);
        verify(sellOrderMapper).insert(sellOrderCaptor.capture());
        SellOrder sellOrder = sellOrderCaptor.getValue();
        assertEquals(2L, sellOrder.getUserId());
        assertEquals(10L, sellOrder.getItemId());
        assertEquals(30L, sellOrder.getInventoryId());
        assertEquals(SellOrder.RESPONSE_YES, sellOrder.getIsResponseToBuy());
        assertEquals(20L, sellOrder.getTargetBuyOrderId());

        verify(userInventoryMapper).updateStatus(30L, UserInventory.STATUS_ON_SALE);
        verify(buyOrderMapper).incrementFilledQuantity(20L);

        InOrder orderVerifier = inOrder(buyOrderMapper, sellOrderMapper, tradeOrderService);
        orderVerifier.verify(buyOrderMapper).incrementFilledQuantity(20L);
        orderVerifier.verify(sellOrderMapper).insert(any(SellOrder.class));
        orderVerifier.verify(tradeOrderService).createOrder(eq(1L), eq(99L), eq(new BigDecimal("8.50")));
    }

    @Test
    void respondToBuyOrderStopsWhenReservationFails() {
        BuyOrder buyOrder = new BuyOrder();
        buyOrder.setId(20L);
        buyOrder.setUserId(1L);
        buyOrder.setItemId(10L);
        buyOrder.setPrice(new BigDecimal("8.50"));
        buyOrder.setQuantity(1);
        buyOrder.setFilledQuantity(0);
        buyOrder.setStatus(BuyOrder.STATUS_ACTIVE);
        buyOrder.setExpireTime(LocalDateTime.now().plusDays(1));

        UserInventory inventory = new UserInventory();
        inventory.setId(30L);
        inventory.setUserId(2L);
        inventory.setItemId(10L);
        inventory.setStatus(UserInventory.STATUS_NORMAL);
        inventory.setIsMarketable(UserInventory.IS_MARKETABLE);

        when(buyOrderMapper.selectById(20L)).thenReturn(buyOrder);
        when(userInventoryMapper.selectById(30L)).thenReturn(inventory);
        when(buyOrderMapper.incrementFilledQuantity(20L)).thenReturn(0);

        assertThrows(RuntimeException.class, () -> buyOrderService.respondToBuyOrder(2L, 20L, 30L));

        verify(sellOrderMapper, never()).insert(any(SellOrder.class));
        verify(userInventoryMapper, never()).updateStatus(eq(30L), eq(UserInventory.STATUS_ON_SALE));
        verify(tradeOrderService, never()).createOrder(any(), any(), any());
    }
}
