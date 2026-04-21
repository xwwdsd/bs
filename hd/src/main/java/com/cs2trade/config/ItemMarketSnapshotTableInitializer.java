package com.cs2trade.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ItemMarketSnapshotTableInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS item_market_snapshot (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    item_id BIGINT NOT NULL,
                    snapshot_date DATE NOT NULL,
                    latest_price DECIMAL(18, 2) DEFAULT NULL,
                    reference_price DECIMAL(18, 2) DEFAULT NULL,
                    lowest_sell_price DECIMAL(18, 2) DEFAULT NULL,
                    avg_trade_price_7d DECIMAL(18, 2) DEFAULT NULL,
                    trade_count_7d INT NOT NULL DEFAULT 0,
                    trade_count_30d INT NOT NULL DEFAULT 0,
                    active_sell_count INT NOT NULL DEFAULT 0,
                    favorite_count INT NOT NULL DEFAULT 0,
                    price_change_7d DECIMAL(10, 2) DEFAULT NULL,
                    price_change_30d DECIMAL(10, 2) DEFAULT NULL,
                    volatility_score INT NOT NULL DEFAULT 0,
                    liquidity_score INT NOT NULL DEFAULT 0,
                    heat_score INT NOT NULL DEFAULT 0,
                    suggested_buy_price DECIMAL(18, 2) DEFAULT NULL,
                    suggested_sell_price DECIMAL(18, 2) DEFAULT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_item_market_snapshot_daily (item_id, snapshot_date),
                    KEY idx_item_market_snapshot_date (snapshot_date),
                    KEY idx_item_market_snapshot_heat (heat_score)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        log.info("item_market_snapshot table is ready");
    }
}
