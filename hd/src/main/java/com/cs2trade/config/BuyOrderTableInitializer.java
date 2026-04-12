package com.cs2trade.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BuyOrderTableInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureTable() {
        jdbcTemplate.execute(
                """
                        CREATE TABLE IF NOT EXISTS buy_order (
                            id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Buy order id',
                            user_id BIGINT UNSIGNED NOT NULL COMMENT 'Buyer user id',
                            item_id BIGINT UNSIGNED NOT NULL COMMENT 'Item id',
                            price DECIMAL(18, 2) NOT NULL COMMENT 'Buy unit price',
                            quantity INT UNSIGNED NOT NULL DEFAULT 1 COMMENT 'Requested quantity',
                            filled_quantity INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Filled quantity',
                            status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0-cancelled, 1-active, 2-completed',
                            expire_time DATETIME NOT NULL COMMENT 'Expire time',
                            auto_accept TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0-no, 1-yes',
                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
                            updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
                            PRIMARY KEY (id),
                            KEY idx_user_id (user_id),
                            KEY idx_item_id (item_id),
                            KEY idx_status (status),
                            KEY idx_expire_time (expire_time)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Buy orders'
                        """
        );

        log.info("buy_order table is ready");
    }
}
