package com.cs2trade.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserTableInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureEmailColumnIsOptional() {
        if (!tableExists("sys_user")) {
            return;
        }

        jdbcTemplate.update("UPDATE sys_user SET email = NULL WHERE TRIM(email) = ''");

        String nullableFlag = jdbcTemplate.queryForObject(
                """
                        SELECT IS_NULLABLE
                        FROM information_schema.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = 'sys_user'
                          AND COLUMN_NAME = 'email'
                        """,
                String.class
        );

        if ("NO".equalsIgnoreCase(nullableFlag)) {
            jdbcTemplate.execute("ALTER TABLE sys_user MODIFY COLUMN email VARCHAR(100) DEFAULT NULL COMMENT '邮箱地址'");
            log.info("Updated sys_user.email to nullable");
        }
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.TABLES
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = ?
                        """,
                Integer.class,
                tableName
        );

        return count != null && count > 0;
    }
}
