package com.cs2trade.service.impl;

import com.cs2trade.service.MarketAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class SteamMarketHistoryCollectorJob {

    private final MarketAnalyticsService marketAnalyticsService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(cron = "${market.analytics.collector-cron:0 10 2 * * *}")
    public void collect() {
        if (!running.compareAndSet(false, true)) {
            log.info("Skip market analytics collector because previous run is still active");
            return;
        }

        try {
            marketAnalyticsService.rebuildAllMarketData();
        } catch (Exception e) {
            log.warn("Market analytics collector failed: {}", e.getMessage(), e);
        } finally {
            running.set(false);
        }
    }
}
