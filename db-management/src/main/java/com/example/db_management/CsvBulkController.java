package com.example.db_management;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class CsvBulkController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    // 全テーブルを1つのCSVファイルでエクスポート
    @GetMapping("/export/all")
    public void exportAll(HttpServletResponse response) throws Exception {
        if ("VIEWER".equals(SecurityUtils.getCurrentRole())) {
            response.setStatus(403);
            return;
        }

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"db-export.csv\"");

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8));

        for (String table : getTables()) {
            List<String> columns = fetchColumns(table);
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM " + table);

            writer.println("#TABLE:" + table);
            writer.println(String.join(",", columns));

            for (Map<String, Object> row : rows) {
                String line = columns.stream()
                        .map(col -> escapeCsv(row.get(col)))
                        .collect(Collectors.joining(","));
                writer.println(line);
            }
            writer.println();
        }
        writer.flush();
    }

    // 1つのCSVファイルから全テーブルを一括インポート
    @PostMapping("/import/all")
    public ResponseEntity<?> importAll(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "append") String mode) throws Exception {

        if ("VIEWER".equals(SecurityUtils.getCurrentRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "権限がありません"));
        }

        List<String> validTables = getTables();
        Map<String, Object> results = new LinkedHashMap<>();

        String content = new String(file.getBytes(), StandardCharsets.UTF_8)
                .replaceAll("^﻿", ""); // BOM除去

        // #TABLE: マーカーでセクション分割
        String[] sections = content.split("(?m)^#TABLE:");

        for (String section : sections) {
            if (section.trim().isEmpty()) continue;

            int newlineIdx = section.indexOf('\n');
            if (newlineIdx < 0) continue;

            String tableName = section.substring(0, newlineIdx).trim().toUpperCase();
            String csvContent = section.substring(newlineIdx + 1);

            if (!validTables.contains(tableName)) {
                results.put(tableName, Map.of("skipped", "テーブルが存在しません"));
                continue;
            }

            try (Reader reader = new StringReader(csvContent);
                 CSVParser parser = CSVFormat.DEFAULT.builder()
                         .setHeader().setSkipHeaderRecord(true)
                         .setIgnoreHeaderCase(true).setTrim(true)
                         .setIgnoreEmptyLines(true).build()
                         .parse(reader)) {

                List<String> csvHeaders = new ArrayList<>(parser.getHeaderMap().keySet());
                List<String> dbColumns = fetchColumns(tableName);

                String invalidCol = csvHeaders.stream()
                        .filter(h -> !dbColumns.contains(h.toUpperCase()))
                        .findFirst().orElse(null);
                if (invalidCol != null) {
                    results.put(tableName, Map.of("error", "カラム '" + invalidCol + "' が存在しません"));
                    continue;
                }

                if ("overwrite".equals(mode)) {
                    jdbcTemplate.execute("DELETE FROM " + tableName);
                }

                String columnList = csvHeaders.stream().map(String::toUpperCase).collect(Collectors.joining(", "));
                String placeholders = csvHeaders.stream().map(h -> "?").collect(Collectors.joining(", "));
                String sql = "INSERT INTO " + tableName + " (" + columnList + ") VALUES (" + placeholders + ")";

                int inserted = 0;
                List<Map<String, Object>> errors = new ArrayList<>();

                for (var record : parser.getRecords()) {
                    try {
                        Object[] values = csvHeaders.stream().map(record::get).toArray();
                        jdbcTemplate.update(sql, values);
                        inserted++;
                    } catch (Exception e) {
                        String rowContent = csvHeaders.stream()
                                .map(h -> record.isMapped(h) ? record.get(h) : "")
                                .collect(Collectors.joining(","));
                        errors.add(Map.of(
                                "line", record.getRecordNumber(),
                                "content", rowContent,
                                "message", e.getMessage() != null ? e.getMessage() : "不明なエラー"
                        ));
                    }
                }

                results.put(tableName, Map.of("inserted", inserted, "errors", errors));
            }
        }

        return ResponseEntity.ok(results);
    }

    private String escapeCsv(Object val) {
        if (val == null) return "";
        String s = val.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private List<String> getTables() throws Exception {
        List<String> tables = new ArrayList<>();
        try (var conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, "PUBLIC", "%", new String[]{"TABLE"})) {
                while (rs.next()) tables.add(rs.getString("TABLE_NAME"));
            }
        }
        return tables;
    }

    private List<String> fetchColumns(String tableName) throws Exception {
        List<String> columns = new ArrayList<>();
        try (var conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(null, "PUBLIC", tableName, "%")) {
                while (rs.next()) columns.add(rs.getString("COLUMN_NAME"));
            }
        }
        return columns;
    }
}
