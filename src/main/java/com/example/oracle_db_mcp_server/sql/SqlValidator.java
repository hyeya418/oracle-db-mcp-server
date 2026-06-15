package com.example.oracle_db_mcp_server.sql;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates that a SQL string is a single, read-only SELECT statement before
 * it is allowed to reach the database. This is the last line of defense
 * against an AI client issuing destructive or multi-statement SQL.
 */
public final class SqlValidator {

    private static final int MAX_ROWS = 100;

    /** Keywords that must never appear anywhere in an accepted query. */
    private static final List<String> FORBIDDEN_KEYWORDS = List.of(
            "INSERT", "UPDATE", "DELETE", "MERGE",
            "DROP", "ALTER", "TRUNCATE", "CREATE", "RENAME",
            "GRANT", "REVOKE",
            "EXEC", "EXECUTE", "CALL",
            "BEGIN", "DECLARE",
            "COMMIT", "ROLLBACK", "SAVEPOINT",
            "LOCK"
    );

    // Matches '--' line comments and /* */ block comments.
    private static final Pattern LINE_COMMENT = Pattern.compile("--.*?(?=\\R|$)");
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    private static final Pattern FETCH_LIMIT = Pattern.compile(
            "FETCH\\s+(FIRST|NEXT)\\s+\\d+\\s+ROWS?\\s+(ONLY|WITH\\s+TIES)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ROWNUM = Pattern.compile("ROWNUM", Pattern.CASE_INSENSITIVE);

    private SqlValidator() {
    }

    /**
     * Validates the given SQL and returns a safe-to-execute version
     * (with a row limit applied if the caller did not specify one).
     *
     * @throws IllegalArgumentException if the SQL is not an acceptable
     *                                   single read-only SELECT statement
     */
    public static String validateAndPrepare(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL이 비어 있습니다.");
        }

        String trimmed = sql.trim();

        // Strip a single trailing semicolon (common from copy-pasted SQL),
        // but reject if another statement follows.
        if (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }

        // Remove comments before any further inspection so keywords cannot
        // be smuggled in via comments, and so a hidden ';' cannot hide there.
        String withoutComments = BLOCK_COMMENT.matcher(trimmed).replaceAll(" ");
        withoutComments = LINE_COMMENT.matcher(withoutComments).replaceAll(" ");
        withoutComments = withoutComments.trim();

        if (withoutComments.isEmpty()) {
            throw new IllegalArgumentException("SQL이 비어 있습니다.");
        }

        // Reject any remaining semicolon -> multiple statements.
        if (withoutComments.contains(";")) {
            throw new IllegalArgumentException("세미콜론(;)으로 여러 쿼리를 연결하는 것은 허용되지 않습니다.");
        }

        // Must start with SELECT or WITH (CTE).
        String upper = withoutComments.toUpperCase();
        if (!(upper.startsWith("SELECT") || upper.startsWith("WITH"))) {
            throw new IllegalArgumentException("SELECT 또는 WITH(CTE)로 시작하는 조회 쿼리만 허용됩니다.");
        }

        // Reject forbidden keywords anywhere in the statement (word-boundary match).
        for (String keyword : FORBIDDEN_KEYWORDS) {
            Pattern p = Pattern.compile("\\b" + keyword + "\\b", Pattern.CASE_INSENSITIVE);
            if (p.matcher(withoutComments).find()) {
                throw new IllegalArgumentException("허용되지 않는 키워드가 포함되어 있습니다: " + keyword);
            }
        }

        // A WITH ... statement must still resolve to a SELECT as its final
        // statement (e.g. "WITH x AS (...) SELECT ..."). Reject CTEs that
        // do not contain a SELECT at all.
        if (upper.startsWith("WITH") && !upper.contains("SELECT")) {
            throw new IllegalArgumentException("WITH 절에는 SELECT 본문이 포함되어야 합니다.");
        }

        // Enforce the row limit if the query doesn't already self-limit.
        if (FETCH_LIMIT.matcher(withoutComments).find() || ROWNUM.matcher(withoutComments).find()) {
            return withoutComments;
        }

        return withoutComments + " FETCH FIRST " + MAX_ROWS + " ROWS ONLY";
    }
}
