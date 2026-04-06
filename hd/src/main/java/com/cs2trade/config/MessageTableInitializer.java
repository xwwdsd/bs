package com.cs2trade.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageTableInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureColumns() {
        addColumnIfMissing(
                "related_sell_order_id",
                "ALTER TABLE sys_message ADD COLUMN related_sell_order_id BIGINT DEFAULT NULL COMMENT '鍏宠仈鍑哄敭璁㈠崟ID' AFTER related_item_id"
        );
        log.info("sys_message table columns are ready");
    }

    private void addColumnIfMissing(String columnName, String alterSql) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = 'sys_message'
                          AND COLUMN_NAME = ?
                        """,
                Integer.class,
                columnName
        );

        if (count == null || count == 0) {
            jdbcTemplate.execute(alterSql);
            log.info("Added column {} to sys_message", columnName);
        }
    }
}
