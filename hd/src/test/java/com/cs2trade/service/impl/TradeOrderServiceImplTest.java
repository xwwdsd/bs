package com.cs2trade.service.impl;

import com.cs2trade.config.SteamTradeBotProperties;
import com.cs2trade.entity.Item;
import com.cs2trade.entity.SellOrder;
import com.cs2trade.entity.TradeOrder;
import com.cs2trade.entity.User;
import com.cs2trade.entity.UserInventory;
import com.cs2trade.mapper.ItemMapper;
import com.cs2trade.mapper.SellOrderMapper;
import com.cs2trade.mapper.TradeOrderMapper;
import com.cs2trade.mapper.UserInventoryMapper;
import com.cs2trade.mapper.UserMapper;
import com.cs2trade.service.MessagePublishService;
import com.cs2trade.service.WalletService;
import com.cs2trade.service.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeOrderServiceImplTest {

    @Mock
    private TradeOrderMapper tradeOrderMapper;

    @Mock
    private SellOrderMapper sellOrderMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ItemMapper itemMapper;

    @Mock
    private UserInventoryMapper userInventoryMapper;

    @Mock
    private WalletService walletService;

    @Mock
    private MessagePublishService messagePublishService;

    @Mock
    private WebSocketService webSocketService;

    @Mock
    private SteamTradeMonitorService steamTradeMonitorService;

    @Mock
    private SteamTradeBotProperties botProperties;

    private TradeOrderServiceImpl tradeOrderService;

    @BeforeEach
    void setUp() {
        tradeOrderService = new TradeOrderServiceImpl(
                tradeOrderMapper,
                sellOrderMapper,
                userMapper,
                itemMapper,
                userInventoryMapper,
                walletService,
                messagePublishService,
                webSocketService,
                steamTradeMonitorService,
                botProperties
        );
    }

    @Test
    void checkBotDeliveryMovesAcceptedOfferToSentWithoutSettlement() {
        TradeOrder offeringOrder = baseOrder(88L);
        offeringOrder.setStatus(TradeOrder.STATUS_OFFERING);
        offeringOrder.setTradeOfferId("offer-001");

        TradeOrder sentOrder = baseOrder(88L);
        sentOrder.setStatus(TradeOrder.STATUS_SENT);
        sentOrder.setTradeOfferId("offer-001");

        when(tradeOrderMapper.selectById(88L)).thenReturn(offeringOrder, sentOrder);
        when(steamTradeMonitorService.inspectBuyerOfferDelivery(offeringOrder))
                .thenReturn(SteamTradeMonitorService.BotDeliveryCheckResult.builder()
                        .offerState(3)
                        .offerStateText("Accepted")
                        .accepted(true)
                        .terminalFailure(false)
                        .inventoryMatched(true)
                        .build());
        when(itemMapper.selectById(11L)).thenReturn(new Item());
        when(userInventoryMapper.selectById(55L)).thenReturn(new UserInventory());
        when(userMapper.selectById(1L)).thenReturn(new User());
        when(userMapper.selectById(2L)).thenReturn(new User());

        TradeOrder result = tradeOrderService.checkBotDelivery(88L, 1L);

        assertEquals(TradeOrder.STATUS_SENT, result.getStatus());

        ArgumentCaptor<TradeOrder> patchCaptor = ArgumentCaptor.forClass(TradeOrder.class);
        verify(tradeOrderMapper).updateBotTracking(patchCaptor.capture());
        TradeOrder patch = patchCaptor.getValue();
        assertEquals(TradeOrder.STATUS_SENT, patch.getStatus());
        assertEquals(TradeOrder.DELIVERY_STAGE_BUYER_RECEIVED, patch.getDeliveryStage());

        verify(walletService, never()).receive(any(), any(), any(), any(), any());
        verify(tradeOrderMapper, never()).updateCompletedStatus(any(), any());
        verify(webSocketService).sendOrderStatusUpdate(
                eq(1L),
                eq(88L),
                eq(TradeOrder.STATUS_SENT),
                any()
        );
        verify(webSocketService).sendOrderStatusUpdate(
                eq(2L),
                eq(88L),
                eq(TradeOrder.STATUS_SENT),
                any()
        );
        verify(messagePublishService, times(2))
                .sendTradeMessage(any(), any(), any(), any(), eq(offeringOrder));
    }

    @Test
    void shipOrderRegistersBuyerSentOfferForPaidOrder() {
        TradeOrder paidOrder = baseOrder(77L);
        paidOrder.setStatus(TradeOrder.STATUS_PAID);

        when(tradeOrderMapper.selectById(77L)).thenReturn(paidOrder);
        when(steamTradeMonitorService.detectBuyerOffer(paidOrder))
                .thenReturn(SteamTradeMonitorService.TradeOfferDetectionResult.found(
                        "offer-002",
                        "https://steamcommunity.com/tradeoffer/offer-002/",
                        2,
                        "Active"
                ));

        boolean success = tradeOrderService.shipOrder(77L, 1L, null, null);

        assertTrue(success);
        verify(tradeOrderMapper).registerBotOffer(
                77L,
                TradeOrder.STATUS_OFFERING,
                "offer-002",
                "https://steamcommunity.com/tradeoffer/offer-002/",
                TradeOrder.DELIVERY_STAGE_BUYER_OFFER_SENT
        );
        verify(webSocketService).sendOrderStatusUpdate(
                eq(1L),
                eq(77L),
                eq(TradeOrder.STATUS_OFFERING),
                any()
        );
        verify(webSocketService).sendOrderStatusUpdate(
                eq(2L),
                eq(77L),
                eq(TradeOrder.STATUS_OFFERING),
                any()
        );
    }

    @Test
    void confirmReceiptCompletesSettlementForSentOrder() {
        TradeOrder sentOrder = baseOrder(99L);
        sentOrder.setStatus(TradeOrder.STATUS_SENT);

        when(tradeOrderMapper.selectById(99L)).thenReturn(sentOrder);
        when(walletService.receive(
                eq(2L),
                eq(new BigDecimal("95.00")),
                any(),
                eq(99L),
                eq("TRADE_ORDER")
        )).thenReturn(true);

        boolean success = tradeOrderService.confirmReceipt(99L, 1L);

        assertTrue(success);
        verify(walletService).receive(
                eq(2L),
                eq(new BigDecimal("95.00")),
                any(),
                eq(99L),
                eq("TRADE_ORDER")
        );
        verify(tradeOrderMapper).updateCompletedStatus(99L, TradeOrder.STATUS_COMPLETED);
        verify(sellOrderMapper).updateStatusByInventoryId(55L, SellOrder.STATUS_SOLD);
        verify(userInventoryMapper).updateStatus(55L, UserInventory.STATUS_SOLD);
        verify(messagePublishService).sendTradeMessage(
                eq(1L),
                eq(3),
                any(),
                any(),
                eq(sentOrder),
                eq(2L),
                any()
        );
        verify(messagePublishService).sendTradeMessage(
                eq(2L),
                eq(3),
                any(),
                any(),
                eq(sentOrder)
        );
    }

    private TradeOrder baseOrder(Long orderId) {
        TradeOrder order = new TradeOrder();
        order.setId(orderId);
        order.setBuyerId(1L);
        order.setSellerId(2L);
        order.setItemId(11L);
        order.setInventoryId(55L);
        order.setOrderNo("TD202604230001");
        order.setPrice(new BigDecimal("100.00"));
        order.setFee(new BigDecimal("5.00"));
        order.setActualAmount(new BigDecimal("95.00"));
        return order;
    }
}
