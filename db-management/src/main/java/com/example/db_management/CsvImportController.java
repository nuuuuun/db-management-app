package com.example.db_management;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/import")
public class CsvImportController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @PostMapping("/{tableName}")
    public ResponseEntity<?> importCsv(
        @PathVariable String tableName,
        @RequestParam("file") MultipartFile file,
        @RequestParam(defaultValue = "append") String mode
    ) throws Exception {

        if ("VIEWER".equals(SecurityUtils.getCurrentRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "権限がありません"));
        }

        String upperTable = tableName.toUpperCase();
        if (!getValidTables().contains(upperTable)) {
            return ResponseEntity.badRequest().body(Map.of("error", "テーブルが存在しません"));
        }

        List<String> dbColumns = getTableColumns(upperTable);

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                 .setHeader().setSkipHeaderRecord(true)
                 .setIgnoreHeaderCase(true).setTrim(true).build()
                 .parse(reader)) {

            List<String> csvHeaders = new ArrayList<>(parser.getHeaderMap().keySet());

            for (String header : csvHeaders) {
                if (!dbColumns.contains(header.toUpperCase())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "カラム '" + header + "' は存在しません"));
                }
            }

            List<CSVRecord> records = parser.getRecords();

            if ("overwrite".equals(mode)) {
                jdbcTemplate.execute("DELETE FROM " + upperTable);
            }

            String columnList = csvHeaders.stream().map(String::toUpperCase).collect(Collectors.joining(", "));
            String placeholders = csvHeaders.stream().map(h -> "?").collect(Collectors.joining(", "));
            String sql = "INSERT INTO " + upperTable + " (" + columnList + ") VALUES (" + placeholders + ")";

            int inserted = 0;
            List<String> errors = new ArrayList<>();

            for (CSVRecord record : records) {
                try {
                    Object[] values = csvHeaders.stream().map(record::get).toArray();
                    jdbcTemplate.update(sql, values);
                    inserted++;
                } catch (Exception e) {
                    errors.add("行 " + record.getRecordNumber() + ": " + e.getMessage());
                }
            }

            return ResponseEntity.ok(Map.of("inserted", inserted, "errors", errors));
        }
    }

    private List<String> getValidTables() throws Exception {
        List<String> tables = new ArrayList<>();
        try (var conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, "PUBLIC", "%", new String[]{"TABLE"})) {
                while (rs.next()) tables.add(rs.getString("TABLE_NAME"));
            }
        }
        return tables;
    }

    private List<String> getTableColumns(String tableName) throws Exception {
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
