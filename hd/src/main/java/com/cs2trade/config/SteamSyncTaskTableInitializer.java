package com.cs2trade.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SteamSyncTaskTableInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS steam_sync_task (
                    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                    task_type VARCHAR(64) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    total_count INT NOT NULL DEFAULT 0,
                    total_pages INT NOT NULL DEFAULT 0,
                    planned_pages INT NOT NULL DEFAULT 0,
                    synced_pages INT NOT NULL DEFAULT 0,
                    current_page INT NOT NULL DEFAULT 0,
                    processed_count INT NOT NULL DEFAULT 0,
                    saved_count INT NOT NULL DEFAULT 0,
                    updated_count INT NOT NULL DEFAULT 0,
                    skipped_count INT NOT NULL DEFAULT 0,
                    remaining_pages INT NOT NULL DEFAULT 0,
                    remaining_items INT NOT NULL DEFAULT 0,
                    max_pages_limit INT NOT NULL DEFAULT 0,
                    failed_page INT DEFAULT NULL,
                    error_message VARCHAR(1000) DEFAULT NULL,
                    started_at DATETIME DEFAULT NULL,
                    finished_at DATETIME DEFAULT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    KEY idx_task_type (task_type),
                    KEY idx_status (status),
                    KEY idx_created_at (created_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """;

        jdbcTemplate.execute(sql);
        log.info("已确认 steam_sync_task 表可用");
    }
}
