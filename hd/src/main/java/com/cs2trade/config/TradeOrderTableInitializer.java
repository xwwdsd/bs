package com.cs2trade.config;

import com.cs2trade.entity.TradeOrder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeOrderTableInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureColumns() {
        addColumnIfMissing("delivery_stage", "ALTER TABLE trade_order ADD COLUMN delivery_stage VARCHAR(32) DEFAULT '" + TradeOrder.DELIVERY_STAGE_NONE + "' COMMENT '机器人交付阶段' AFTER trade_offer_url");
        addColumnIfMissing("steam_offer_state", "ALTER TABLE trade_order ADD COLUMN steam_offer_state INT DEFAULT NULL COMMENT 'Steam报价状态码' AFTER delivery_stage");
        addColumnIfMissing("steam_offer_state_text", "ALTER TABLE trade_order ADD COLUMN steam_offer_state_text VARCHAR(64) DEFAULT NULL COMMENT 'Steam报价状态文本' AFTER steam_offer_state");
        addColumnIfMissing("bot_offer_dispatched_at", "ALTER TABLE trade_order ADD COLUMN bot_offer_dispatched_at DATETIME DEFAULT NULL COMMENT '机器人报价发出时间' AFTER steam_offer_state_text");
        addColumnIfMissing("seller_offer_confirmed_at", "ALTER TABLE trade_order ADD COLUMN seller_offer_confirmed_at DATETIME DEFAULT NULL COMMENT '卖家确认报价时间' AFTER bot_offer_dispatched_at");
        addColumnIfMissing("last_offer_check_at", "ALTER TABLE trade_order ADD COLUMN last_offer_check_at DATETIME DEFAULT NULL COMMENT '最近一次检查报价时间' AFTER seller_offer_confirmed_at");
        addColumnIfMissing("inventory_verified_at", "ALTER TABLE trade_order ADD COLUMN inventory_verified_at DATETIME DEFAULT NULL COMMENT '检测到对方库存出现饰品时间' AFTER last_offer_check_at");
        addColumnIfMissing("bot_received_at", "ALTER TABLE trade_order ADD COLUMN bot_received_at DATETIME DEFAULT NULL COMMENT '机器人确认收到饰品时间' AFTER inventory_verified_at");
        addColumnIfMissing("monitor_error_message", "ALTER TABLE trade_order ADD COLUMN monitor_error_message VARCHAR(500) DEFAULT NULL COMMENT '监控错误信息' AFTER bot_received_at");

        log.info("trade_order table columns are ready");
    }

    private void addColumnIfMissing(String columnName, String alterSql) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = 'trade_order'
                          AND COLUMN_NAME = ?
                        """,
                Integer.class,
                columnName
        );

        if (count == null || count == 0) {
            jdbcTemplate.execute(alterSql);
            log.info("Added column {} to trade_order", columnName);
        }
    }
}
