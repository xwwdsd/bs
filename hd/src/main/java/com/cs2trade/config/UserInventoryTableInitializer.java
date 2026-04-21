package com.cs2trade.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserInventoryTableInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureColumns() {
        addColumnIfMissing("market_hash_name", "ALTER TABLE user_inventory ADD COLUMN market_hash_name VARCHAR(255) DEFAULT NULL COMMENT 'Steam Market Hash Name' AFTER instance_id");
        addColumnIfMissing("icon_url_large", "ALTER TABLE user_inventory ADD COLUMN icon_url_large VARCHAR(255) DEFAULT NULL COMMENT '物品大图URL' AFTER icon_url");
        addColumnIfMissing("paint_index", "ALTER TABLE user_inventory ADD COLUMN paint_index INT UNSIGNED DEFAULT NULL COMMENT '皮肤编号 / Finish Catalog' AFTER paint_seed");
        addColumnIfMissing("inspect_url", "ALTER TABLE user_inventory ADD COLUMN inspect_url VARCHAR(1000) DEFAULT NULL COMMENT '检视链接' AFTER paint_wear");
        addColumnIfMissing("description", "ALTER TABLE user_inventory ADD COLUMN description TEXT DEFAULT NULL COMMENT '物品描述' AFTER inspect_url");
        addColumnIfMissing("marketable_reason", "ALTER TABLE user_inventory ADD COLUMN marketable_reason VARCHAR(255) DEFAULT NULL COMMENT '不可出售原因' AFTER description");
        addColumnIfMissing("rarity", "ALTER TABLE user_inventory ADD COLUMN rarity VARCHAR(64) DEFAULT NULL COMMENT '前端展示用稀有度' AFTER marketable_reason");
        addColumnIfMissing("type", "ALTER TABLE user_inventory ADD COLUMN type VARCHAR(64) DEFAULT NULL COMMENT '前端展示用类型' AFTER rarity");

        ensureVarcharLength("icon_url", 1024, "ALTER TABLE user_inventory MODIFY COLUMN icon_url VARCHAR(1024) DEFAULT NULL COMMENT '鐗╁搧鍥炬爣URL'");
        ensureVarcharLength("icon_url_large", 1024, "ALTER TABLE user_inventory MODIFY COLUMN icon_url_large VARCHAR(1024) DEFAULT NULL COMMENT '鐗╁搧澶у浘URL'");

        log.info("user_inventory table columns are ready");
    }

    private void addColumnIfMissing(String columnName, String alterSql) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = 'user_inventory'
                          AND COLUMN_NAME = ?
                        """,
                Integer.class,
                columnName
        );

        if (count == null || count == 0) {
            jdbcTemplate.execute(alterSql);
            log.info("Added column {} to user_inventory", columnName);
        }
    }
    private void ensureVarcharLength(String columnName, int minLength, String alterSql) {
        Integer currentLength = jdbcTemplate.queryForObject(
                """
                        SELECT CHARACTER_MAXIMUM_LENGTH
                        FROM information_schema.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = 'user_inventory'
                          AND COLUMN_NAME = ?
                        """,
                Integer.class,
                columnName
        );

        if (currentLength != null && currentLength < minLength) {
            jdbcTemplate.execute(alterSql);
            log.info("Expanded user_inventory.{} length from {} to {}", columnName, currentLength, minLength);
        }
    }
}
