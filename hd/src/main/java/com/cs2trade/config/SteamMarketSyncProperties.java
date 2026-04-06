package com.cs2trade.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "steam.market-sync")
public class SteamMarketSyncProperties {

    private int pageSize = 100;

    private int maxPagesPerRun = 200;

    private List<Integer> rateLimitBackoffSeconds = new ArrayList<>(List.of(60, 120, 240, 300, 300));

    private int autoRetryBudgetMinutes = 30;

    private int pageDelayMillis = 5000;
}
