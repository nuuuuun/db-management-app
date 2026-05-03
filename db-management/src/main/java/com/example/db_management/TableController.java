package com.example.db_management;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/tables")
public class TableController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MaskedColumnRepository maskedColumnRepository;

    @Autowired
    private OperationHistoryService operationHistoryService;

    private static final List<String> TABLE_ORDER = List.of(
        "PROJECTS", "REQUIREMENTS", "SPECIFICATIONS", "APPLICATIONS", "ENVIRONMENTS"
    );

    @GetMapping
    public List<String> getTables() throws Exception {
        List<String> all = new ArrayList<>();
        try (var conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, "PUBLIC", "%", new String[]{"TABLE"})) {
                while (rs.next()) all.add(rs.getString("TABLE_NAME"));
            }
        }
        List<String> ordered = new ArrayList<>(TABLE_ORDER);
        ordered.retainAll(all);
        all.stream().filter(t -> !TABLE_ORDER.contains(t)).forEach(ordered::add);
        return ordered;
    }

    @GetMapping("/{tableName}/columns")
    public ResponseEntity<List<String>> getColumns(@PathVariable String tableName) throws Exception {
        if (!getTables().contains(tableName.toUpperCase())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(fetchColumns(tableName.toUpperCase()));
    }

    @GetMapping("/{tableName}")
    public ResponseEntity<Map<String, Object>> getTableData(
            @PathVariable String tableName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sortCol,
            @RequestParam(defaultValue = "asc") String sortDir) throws Exception {

        if (!getTables().contains(tableName.toUpperCase())) {
            return ResponseEntity.notFound().build();
        }
        size = Math.min(size, 100);

        String upper = tableName.toUpperCase();
        List<String> columns = fetchColumns(upper);

        // WHERE句（全カラムをキーワード検索）
        String where = "";
        List<Object> params = new ArrayList<>();
        if (search != null && !search.isBlank()) {
            String like = "%" + search.toLowerCase() + "%";
            String conditions = columns.stream()
                    .map(c -> "LOWER(CAST(" + c + " AS VARCHAR)) LIKE ?")
                    .collect(Collectors.joining(" OR "));
            where = " WHERE " + conditions;
            columns.forEach(c -> params.add(like));
        }

        // ORDER BY句（カラム名をホワイトリスト検証）
        String orderBy = "";
        if (sortCol != null && !sortCol.isBlank()) {
            String upperCol = sortCol.toUpperCase();
            if (columns.contains(upperCol)) {
                String dir = "desc".equalsIgnoreCase(sortDir) ? "DESC" : "ASC";
                orderBy = " ORDER BY " + upperCol + " " + dir;
            }
        }

        int offset = page * size;
        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(size);
        pageParams.add(offset);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM " + upper + where + orderBy + " LIMIT ? OFFSET ?",
                pageParams.toArray());
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + upper + where, params.toArray(), Integer.class);

        // カラムマスキング
        String role = SecurityUtils.getCurrentRole();
        Set<String> maskedCols = new HashSet<>();
        if (!"ADMIN".equals(role)) {
            List<String> rolesToCheck = "VIEWER".equals(role)
                    ? List.of("VIEWER", "EDITOR")
                    : List.of("EDITOR");
            maskedColumnRepository
                    .findByTableNameAndRoleIn(upper, rolesToCheck)
                    .stream()
                    .map(MaskedColumn::getColumnName)
                    .forEach(maskedCols::add);
        }

        List<String> visibleColumns = columns.stream()
                .filter(col -> !maskedCols.contains(col))
                .collect(Collectors.toList());

        List<Map<String, Object>> filteredRows = rows.stream()
                .map(row -> {
                    Map<String, Object> filtered = new LinkedHashMap<>(row);
                    maskedCols.forEach(filtered::remove);
                    return filtered;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("columns", visibleColumns);
        result.put("rows", filteredRows);
        result.put("total", total != null ? total : 0);
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", total != null ? (int) Math.ceil((double) total / size) : 1);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{tableName}/rows")
    public ResponseEntity<?> insertRow(
            @PathVariable String tableName,
            @RequestBody Map<String, Object> body) throws Exception {

        if ("VIEWER".equals(SecurityUtils.getCurrentRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "権限がありません"));
        }
        String upper = tableName.toUpperCase();
        if (!getTables().contains(upper)) {
            return ResponseEntity.badRequest().body(Map.of("error", "テーブルが存在しません"));
        }

        List<String> dbCols = fetchColumns(upper);
        List<String> cols = body.keySet().stream()
                .filter(k -> dbCols.contains(k.toUpperCase()))
                .collect(Collectors.toList());

        if (cols.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "有効なカラムがありません"));
        }

        String columnList = cols.stream().map(String::toUpperCase).collect(Collectors.joining(", "));
        String placeholders = cols.stream().map(c -> "?").collect(Collectors.joining(", "));
        String sql = "INSERT INTO " + upper + " (" + columnList + ") VALUES (" + placeholders + ")";
        Object[] values = cols.stream().map(body::get).toArray();
        jdbcTemplate.update(sql, values);
        String afterText = cols.stream()
                .map(c -> c.toUpperCase() + ": " + (body.get(c) != null ? body.get(c) : ""))
                .collect(Collectors.joining("\n"));
        operationHistoryService.log(SecurityUtils.getCurrentUsername(), upper, "-", "INSERT", null, afterText);
        return ResponseEntity.ok(Map.of("message", "追加しました"));
    }

    @PutMapping("/{tableName}/rows/{id}")
    public ResponseEntity<?> updateRow(
            @PathVariable String tableName,
            @PathVariable String id,
            @RequestBody Map<String, Object> body) throws Exception {

        if ("VIEWER".equals(SecurityUtils.getCurrentRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "権限がありません"));
        }
        String upper = tableName.toUpperCase();
        if (!getTables().contains(upper)) {
            return ResponseEntity.badRequest().body(Map.of("error", "テーブルが存在しません"));
        }

        String pkCol = getPrimaryKeyColumn(upper);
        List<String> dbCols = fetchColumns(upper);
        List<String> cols = body.keySet().stream()
                .filter(k -> dbCols.contains(k.toUpperCase()) && !k.toUpperCase().equals(pkCol))
                .collect(Collectors.toList());

        if (cols.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "更新するカラムがありません"));
        }

        List<Map<String, Object>> beforeRows = jdbcTemplate.queryForList(
                "SELECT * FROM " + upper + " WHERE " + pkCol + " = ?", id);
        Map<String, Object> beforeRow = beforeRows.isEmpty() ? Map.of() : beforeRows.get(0);

        String setClause = cols.stream().map(c -> c.toUpperCase() + " = ?").collect(Collectors.joining(", "));
        String sql = "UPDATE " + upper + " SET " + setClause + " WHERE " + pkCol + " = ?";
        Object[] values = Stream.concat(cols.stream().map(body::get), Stream.of(id)).toArray();
        jdbcTemplate.update(sql, values);

        // 変更があったカラムのみ記録
        List<String> changedCols = cols.stream().filter(c -> {
            Object before = beforeRow.get(c.toUpperCase());
            Object after = body.get(c);
            String b = before != null ? before.toString() : "";
            String a = after != null ? after.toString() : "";
            return !b.equals(a);
        }).collect(Collectors.toList());

        String beforeText = changedCols.stream()
                .map(c -> c.toUpperCase() + ": " + (beforeRow.get(c.toUpperCase()) != null ? beforeRow.get(c.toUpperCase()) : ""))
                .collect(Collectors.joining("\n"));
        String afterText = changedCols.stream()
                .map(c -> c.toUpperCase() + ": " + (body.get(c) != null ? body.get(c) : ""))
                .collect(Collectors.joining("\n"));

        operationHistoryService.log(SecurityUtils.getCurrentUsername(), upper, id, "UPDATE",
                beforeText.isEmpty() ? null : beforeText,
                afterText.isEmpty() ? null : afterText);
        return ResponseEntity.ok(Map.of("message", "更新しました"));
    }

    @DeleteMapping("/{tableName}/rows/{id}")
    public ResponseEntity<?> deleteRow(
            @PathVariable String tableName,
            @PathVariable String id) throws Exception {

        if ("VIEWER".equals(SecurityUtils.getCurrentRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "権限がありません"));
        }
        String upper = tableName.toUpperCase();
        if (!getTables().contains(upper)) {
            return ResponseEntity.badRequest().body(Map.of("error", "テーブルが存在しません"));
        }

        String pkCol = getPrimaryKeyColumn(upper);
        List<Map<String, Object>> beforeRows = jdbcTemplate.queryForList(
                "SELECT * FROM " + upper + " WHERE " + pkCol + " = ?", id);
        String beforeText = null;
        if (!beforeRows.isEmpty()) {
            Map<String, Object> beforeRow = beforeRows.get(0);
            beforeText = beforeRow.entrySet().stream()
                    .map(e -> e.getKey() + ": " + (e.getValue() != null ? e.getValue() : ""))
                    .collect(Collectors.joining("\n"));
        }

        jdbcTemplate.update("DELETE FROM " + upper + " WHERE " + pkCol + " = ?", id);
        operationHistoryService.log(SecurityUtils.getCurrentUsername(), upper, id, "DELETE", beforeText, null);
        return ResponseEntity.ok(Map.of("message", "削除しました"));
    }

    private String getPrimaryKeyColumn(String tableName) throws Exception {
        try (var conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getPrimaryKeys(null, "PUBLIC", tableName)) {
                if (rs.next()) return rs.getString("COLUMN_NAME");
            }
        }
        return "ID";
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
