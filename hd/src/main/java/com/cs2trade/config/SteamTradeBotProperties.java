package com.cs2trade.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "steam.bot")
public class SteamTradeBotProperties {

    private boolean enabled = false;
    private String apiKey;
    private String steamId;
    private int pollIntervalMillis = 60000;
    private int batchSize = 20;
}
