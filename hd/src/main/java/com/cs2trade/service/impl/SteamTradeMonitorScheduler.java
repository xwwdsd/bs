package com.cs2trade.service.impl;

import com.cs2trade.config.SteamTradeBotProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class SteamTradeMonitorScheduler {

    private final SteamTradeBotProperties botProperties;
    private final TradeOrderServiceImpl tradeOrderService;
    private final SteamTradeMonitorService steamTradeMonitorService;
    private final AtomicBoolean polling = new AtomicBoolean(false);

    @Scheduled(fixedDelayString = "${steam.bot.poll-interval-millis:60000}")
    public void pollOfferingOrders() {
        if (!botProperties.isEnabled()) {
            return;
        }

        if (!steamTradeMonitorService.isBotMonitoringEnabled()) {
            log.debug("Steam 交易监控未启用或缺少配置，跳过轮询");
            return;
        }

        if (!polling.compareAndSet(false, true)) {
            return;
        }

        try {
            tradeOrderService.pollPendingShipmentOrders();
            tradeOrderService.pollOfferingOrders();
        } catch (Exception e) {
            log.warn("Steam 交易监控轮询失败: {}", e.getMessage());
        } finally {
            polling.set(false);
        }
    }
}
