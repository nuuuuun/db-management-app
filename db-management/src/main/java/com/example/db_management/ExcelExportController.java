package com.example.db_management;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/export")
public class ExcelExportController {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private DataSource dataSource;
    @Autowired private MaskedColumnRepository maskedColumnRepository;

    private static final Map<String, String> TABLE_LABELS = Map.of(
        "PROJECTS",          "案件管理",
        "REQUIREMENTS",      "要求管理",
        "SPECIFICATIONS",    "要件管理",
        "APPLICATIONS",      "アプリ管理",
        "ENVIRONMENTS",      "環境管理",
        "OPERATION_HISTORY", "操作ヒストリー"
    );

    private static final Map<String, Map<String, String>> COLUMN_LABELS;
    static {
        Map<String, Map<String, String>> m = new LinkedHashMap<>();
        m.put("PROJECTS", linkedMap(
            "ID","ID","TOROKU_DATE","登録日","ANKEN_ID","案件ID","ANKEN_NAME","案件名",
            "JIRA_LINK","Jiraリンク","TR_JIRA_LINK","TRJiraリンク","KOUTEI","工程",
            "STATUS","status","GAIYOU","概要","ITAKU_NO","業務委託管理No."));
        m.put("REQUIREMENTS", linkedMap(
            "ID","ID","TOROKU_DATE","登録日","ANKEN_NAME","案件名","YOQYU_MOTO","要求元",
            "YOQYU_ID","要求ID","YOQYU_SHIYOU","要求仕様","YOQYU_NAME","要求名",
            "STATUS","status","HAIKEI","背景理由やリンク等","YOQYU_GAIYOU","要求概要",
            "JIRA_LINK","Jiraリンク","SHIRYO_LINK","資料リンク"));
        m.put("SPECIFICATIONS", linkedMap(
            "ID","ID","TOROKU_DATE","登録日","YOQYU_ID","要求ID","YOKEN_ID","要件ID",
            "YOKEN_GAIYOU","要件概要","JIRA_LINK","Jiraリンク","STATUS","status","BIKO","備考"));
        m.put("APPLICATIONS", linkedMap(
            "ID","ID","TOROKU_DATE","登録日","APP_ID","アプリID","APP_NAME","アプリ名",
            "KANRYO_DATE","完了日","BIKO","備考","DIFF_VER","DIFF_Ver","CVOS_VER","CVOS_Ver","KTS_VER","KTS_Ver"));
        m.put("ENVIRONMENTS", linkedMap(
            "ID","ID","ENV_ID","環境ID","KANKYO","環境","EDABAN","枝番","ENV_NAME","環境名",
            "DEPLOY_PLAN_DATE","デプロイ予定日","DEPLOY_DONE_DATE","デプロイ完了日",
            "APP_ID","アプリID","APP_NAME","アプリ名","TOKYO","東京","OSAKA","大阪"));
        m.put("OPERATION_HISTORY", linkedMap(
            "ID","ID","OPERATED_AT","操作日時","OPERATOR","操作者",
            "TARGET_TABLE","対象テーブル","TARGET_ID","対象ID",
            "OPERATION","操作内容","BEFORE_VALUE","変更前","AFTER_VALUE","変更後"));
        COLUMN_LABELS = Collections.unmodifiableMap(m);
    }

    private static final List<String> TABLE_ORDER = List.of(
        "PROJECTS", "REQUIREMENTS", "SPECIFICATIONS", "APPLICATIONS", "ENVIRONMENTS"
    );

    private static Map<String, String> linkedMap(String... pairs) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) m.put(pairs[i], pairs[i + 1]);
        return m;
    }

    @GetMapping("/excel/{tableName}")
    public void exportTableExcel(
            @PathVariable String tableName,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String filterCol,
            HttpServletResponse response) throws Exception {

        String upper = tableName.toUpperCase();
        if (!getTables().contains(upper)) { response.setStatus(404); return; }

        String displayName = TABLE_LABELS.getOrDefault(upper, upper);
        String encoded = URLEncoder.encode(displayName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded + ".xlsx");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            writeSheet(wb, upper, search, filterCol);
            wb.write(response.getOutputStream());
        }
    }

    @GetMapping("/excel/all")
    public void exportAllExcel(HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"db-export-all.xlsx\"");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            List<String> tables = getTables();
            List<String> ordered = new ArrayList<>(TABLE_ORDER);
            ordered.retainAll(tables);
            tables.stream().filter(t -> !TABLE_ORDER.contains(t)).forEach(ordered::add);
            for (String table : ordered) writeSheet(wb, table, null, null);
            wb.write(response.getOutputStream());
        }
    }

    private void writeSheet(XSSFWorkbook wb, String tableName, String search, String filterCol) throws Exception {
        String displayName = TABLE_LABELS.getOrDefault(tableName, tableName);
        Sheet sheet = wb.createSheet(displayName);

        List<String> columns = fetchColumns(tableName);

        // カラムマスキング
        String role = SecurityUtils.getCurrentRole();
        Set<String> maskedCols = new HashSet<>();
        if (!"ADMIN".equals(role)) {
            List<String> rolesToCheck = "VIEWER".equals(role) ? List.of("VIEWER", "EDITOR") : List.of("EDITOR");
            maskedColumnRepository.findByTableNameAndRoleIn(tableName, rolesToCheck)
                .stream().map(MaskedColumn::getColumnName).forEach(maskedCols::add);
        }
        List<String> visible = columns.stream().filter(c -> !maskedCols.contains(c)).collect(Collectors.toList());

        Map<String, String> colMap = COLUMN_LABELS.getOrDefault(tableName, Map.of());

        // ヘッダースタイル（青背景・白太字）
        XSSFCellStyle headerStyle = wb.createCellStyle();
        XSSFFont headerFont = wb.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 58, (byte) 90, (byte) 138}, null));
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 偶数行スタイル（薄青）
        XSSFCellStyle evenStyle = wb.createCellStyle();
        evenStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 232, (byte) 244, (byte) 252}, null));
        evenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < visible.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(colMap.getOrDefault(visible.get(i), visible.get(i)));
            cell.setCellStyle(headerStyle);
        }

        // WHERE句（検索フィルター）
        List<Object> params = new ArrayList<>();
        String where = "";
        if (search != null && !search.isBlank()) {
            String like = "%" + search.toLowerCase() + "%";
            if (filterCol != null && !filterCol.isBlank() && columns.contains(filterCol.toUpperCase())) {
                where = " WHERE LOWER(CAST(" + filterCol.toUpperCase() + " AS VARCHAR)) LIKE ?";
                params.add(like);
            } else {
                String cond = columns.stream()
                    .map(c -> "LOWER(CAST(" + c + " AS VARCHAR)) LIKE ?")
                    .collect(Collectors.joining(" OR "));
                where = " WHERE " + cond;
                columns.forEach(c -> params.add(like));
            }
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM " + tableName + where, params.toArray());

        for (int r = 0; r < rows.size(); r++) {
            Row row = sheet.createRow(r + 1);
            Map<String, Object> data = rows.get(r);
            for (int c = 0; c < visible.size(); c++) {
                Cell cell = row.createCell(c);
                if ((r + 1) % 2 == 0) cell.setCellStyle(evenStyle);
                Object val = data.get(visible.get(c));
                if (val != null) cell.setCellValue(val.toString());
            }
        }

        for (int i = 0; i < visible.size(); i++) sheet.autoSizeColumn(i);
    }

    private List<String> getTables() throws Exception {
        List<String> tables = new ArrayList<>();
        try (var conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, "public", "%", new String[]{"TABLE"})) {
                while (rs.next()) tables.add(rs.getString("TABLE_NAME").toUpperCase());
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
