package com.example.db_management;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;

@Service
public class OperationHistoryService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void log(String operator, String targetTable, String targetId, String operation,
                    String beforeValue, String afterValue) {
        try {
            jdbcTemplate.update(
                "INSERT INTO OPERATION_HISTORY (OPERATED_AT, OPERATOR, TARGET_TABLE, TARGET_ID, OPERATION, BEFORE_VALUE, AFTER_VALUE) VALUES (?, ?, ?, ?, ?, ?, ?)",
                Timestamp.from(Instant.now()),
                operator,
                targetTable,
                targetId,
                operation,
                beforeValue,
                afterValue
            );
        } catch (Exception ignored) {
        }
    }
}
