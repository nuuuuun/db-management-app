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

    @Autowired
    private MaskedColumnRepository maskedColumnRepository;

    // 業務テーブルのみ許可（管理テーブルを除外）
    private static final Set<String> ALLOWED_TABLES = Set.of(
        "PROJECTS", "REQUIREMENTS", "SPECIFICATIONS", "APPLICATIONS", "ENVIRONMENTS"
    );

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

        String role = SecurityUtils.getCurrentRole();

        for (String table : getAllowedTables()) {
            List<String> columns = fetchColumns(table);

            // マスク処理：ロールに応じてカラムを除外
            Set<String> maskedCols = new HashSet<>();
            if (!"ADMIN".equals(role)) {
                List<String> rolesToCheck = "VIEWER".equals(role)
                        ? List.of("VIEWER", "EDITOR")
                        : List.of("EDITOR");
                maskedColumnRepository.findByTableNameAndRoleIn(table, rolesToCheck)
                        .stream().map(MaskedColumn::getColumnName).forEach(maskedCols::add);
            }
            List<String> visibleColumns = columns.stream()
                    .filter(c -> !maskedCols.contains(c))
                    .collect(Collectors.toList());

            List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM " + table);

            writer.println("#TABLE:" + table);
            writer.println(String.join(",", visibleColumns));

            for (Map<String, Object> row : rows) {
                String line = visibleColumns.stream()
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

            if (!ALLOWED_TABLES.contains(tableName)) {
                results.put(tableName, Map.of("skipped", "対象外のテーブルです"));
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
                                "message", "インポートエラーが発生しました"
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

    private List<String> getAllowedTables() throws Exception {
        List<String> tables = new ArrayList<>();
        try (var conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, "public", "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String name = rs.getString("TABLE_NAME").toUpperCase();
                    if (ALLOWED_TABLES.contains(name)) tables.add(name);
                }
            }
        }
        return tables;
    }

    private List<String> fetchColumns(String tableName) throws Exception {
        List<String> columns = new ArrayList<>();
        try (var conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(null, "public", tableName.toLowerCase(), "%")) {
                while (rs.next()) columns.add(rs.getString("COLUMN_NAME").toUpperCase());
            }
        }
        return columns;
    }
}
