package com.example.oracle_db_mcp_server.tool;

import com.example.oracle_db_mcp_server.sql.SqlValidator;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DbQueryTool {

    private final JdbcTemplate jdbcTemplate;

    public DbQueryTool(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Tool(description = "현재 접속 계정(스키마)이 소유한 테이블 목록을 조회한다.")
    public String listTables() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM USER_TABLES ORDER BY TABLE_NAME");

        if (rows.isEmpty()) {
            return "조회된 테이블이 없습니다.";
        }

        return rows.stream()
                .map(row -> String.valueOf(row.get("TABLE_NAME")))
                .collect(Collectors.joining("\n"));
    }

    @Tool(description = "지정한 테이블의 컬럼명, 데이터 타입, NULL 허용 여부를 조회한다.")
    public String describeTable(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return "테이블명을 입력해 주세요.";
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME, DATA_TYPE, DATA_LENGTH, NULLABLE " +
                        "FROM USER_TAB_COLUMNS " +
                        "WHERE TABLE_NAME = ? " +
                        "ORDER BY COLUMN_ID",
                tableName.toUpperCase());

        if (rows.isEmpty()) {
            return "테이블 '" + tableName + "'을 찾을 수 없거나 컬럼 정보가 없습니다.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-30s %-15s %-10s %s%n", "COLUMN_NAME", "DATA_TYPE", "LENGTH", "NULLABLE"));
        for (Map<String, Object> row : rows) {
            sb.append(String.format("%-30s %-15s %-10s %s%n",
                    row.get("COLUMN_NAME"),
                    row.get("DATA_TYPE"),
                    row.get("DATA_LENGTH"),
                    "Y".equals(row.get("NULLABLE")) ? "YES" : "NO"));
        }
        return sb.toString();
    }

    @Tool(description = "SELECT 조회 쿼리를 실행하고 결과를 표 형태 문자열로 반환한다. " +
            "SELECT/WITH로 시작하는 단일 조회 쿼리만 허용되며, 결과는 최대 100행으로 제한된다.")
    public String executeQuery(String sql) {
        String safeSql;
        try {
            safeSql = SqlValidator.validateAndPrepare(sql);
        } catch (IllegalArgumentException e) {
            return "쿼리가 거부되었습니다: " + e.getMessage();
        }

        List<Map<String, Object>> rows;
        try {
            rows = jdbcTemplate.queryForList(safeSql);
        } catch (Exception e) {
            return "쿼리 실행 중 오류가 발생했습니다: " + e.getMessage();
        }

        if (rows.isEmpty()) {
            return "조회 결과가 없습니다.";
        }

        List<String> columns = List.copyOf(rows.get(0).keySet());

        StringBuilder sb = new StringBuilder();
        sb.append(String.join(" | ", columns)).append("\n");
        for (Map<String, Object> row : rows) {
            sb.append(columns.stream()
                            .map(col -> String.valueOf(row.get(col)))
                            .collect(Collectors.joining(" | ")))
                    .append("\n");
        }
        return sb.toString();
    }
}
