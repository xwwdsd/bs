package com.cs2trade.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ItemTableInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureColumns() {
        addColumnIfMissing(
                "steam_reference_price",
                "ALTER TABLE item ADD COLUMN steam_reference_price DECIMAL(18, 2) DEFAULT NULL COMMENT 'Steam reference price' AFTER buff_price"
        );
        addColumnIfMissing(
                "steam_reference_currency",
                "ALTER TABLE item ADD COLUMN steam_reference_currency VARCHAR(8) DEFAULT 'CNY' COMMENT 'Steam reference price currency' AFTER steam_reference_price"
        );
        addColumnIfMissing(
                "steam_reference_price_source",
                "ALTER TABLE item ADD COLUMN steam_reference_price_source VARCHAR(32) DEFAULT NULL COMMENT 'Steam reference price source' AFTER steam_reference_currency"
        );
        addColumnIfMissing(
                "steam_reference_price_updated_at",
                "ALTER TABLE item ADD COLUMN steam_reference_price_updated_at DATETIME DEFAULT NULL COMMENT 'Steam reference price update time' AFTER steam_reference_price_source"
        );

        backfillLegacyBuffPrice();
        log.info("item table reference price columns are ready");
    }

    private void addColumnIfMissing(String columnName, String alterSql) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = 'item'
                          AND COLUMN_NAME = ?
                        """,
                Integer.class,
                columnName
        );

        if (count == null || count == 0) {
            jdbcTemplate.execute(alterSql);
            log.info("Added column {} to item", columnName);
        }
    }

    private void backfillLegacyBuffPrice() {
        int updated = jdbcTemplate.update(
                """
                        UPDATE item
                        SET steam_reference_price = buff_price,
                            steam_reference_currency = 'CNY',
                            steam_reference_price_source = 'legacy_buff_price',
                            steam_reference_price_updated_at = COALESCE(updated_at, NOW())
                        WHERE steam_reference_price IS NULL
                          AND buff_price IS NOT NULL
                          AND buff_price > 0
                        """
        );
        if (updated > 0) {
            log.info("Backfilled {} item Steam reference prices from buff_price", updated);
        }
    }
}
