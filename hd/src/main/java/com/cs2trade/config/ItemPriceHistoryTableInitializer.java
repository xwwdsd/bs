package com.cs2trade.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ItemPriceHistoryTableInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS item_price_history (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    item_id BIGINT NOT NULL,
                    source VARCHAR(32) NOT NULL,
                    price DECIMAL(18, 2) NOT NULL,
                    volume INT DEFAULT NULL,
                    recorded_at DATETIME NOT NULL,
                    range_type VARCHAR(16) NOT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    KEY idx_item_price_history_item_source (item_id, source),
                    KEY idx_item_price_history_recorded_at (recorded_at),
                    UNIQUE KEY uk_item_price_history_unique (item_id, source, range_type, recorded_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        log.info("item_price_history table is ready");
    }
}
