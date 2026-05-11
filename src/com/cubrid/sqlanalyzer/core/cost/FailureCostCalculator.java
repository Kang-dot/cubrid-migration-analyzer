package com.cubrid.sqlanalyzer.core.cost;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cubrid.sqlanalyzer.command.AnalyzerConsoleFailure;
import com.cubrid.sqlanalyzer.command.AnalyzerConsoleReport;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes;

public class FailureCostCalculator implements AnalyzerCostCalculator {
    private static final float TABLE_BASE_COST = 0.1f;
    private static final float VIEW_BASE_COST = 1.0f;
    private static final float INDEX_BASE_COST = 0.1f;
    private static final float SEQUENCE_BASE_COST = 0.1f;
    private static final float SYNONYM_BASE_COST = 0.1f;
    private static final float FUNCTION_BASE_COST = 1.0f;
    private static final float PROCEDURE_BASE_COST = 1.0f;
    private static final float DML_BASE_COST = 0.2f;
    private static final float GRANT_BASE_COST = 0.1f;
    private static final float PK_BASE_COST = 0.1f;
    private static final float FK_BASE_COST = 0.1f;

    @Override
    public void analyzeAfterExecution(AnalyzerConsoleReport report) {
        for (AnalyzerConsoleFailure failure : report.getFailures()) {
            failure.setEstimatedCost(calculateCostByType(failure.getStatementType(), failure.getSql()));
        }
    }

    private float calculateCostByType(String statementType, String sql) {
        if (statementType == null) {
            return 0.0f;
        }

        switch (statementType) {
            case AnalyzerStatementTypes.TYPE_DDL_TABLE:
                return calculateTable(sql);
            case AnalyzerStatementTypes.TYPE_DDL_VIEW:
            case AnalyzerStatementTypes.TYPE_DDL_VIEW_CREATE:
            case AnalyzerStatementTypes.TYPE_DDL_VIEW_ALTER:
                return calculateView(sql);
            case AnalyzerStatementTypes.TYPE_DDL_INDEX:
                return calculateIndex(sql);
            case AnalyzerStatementTypes.TYPE_DDL_SEQUENCE:
                return calculateSequence();
            case AnalyzerStatementTypes.TYPE_DDL_SYNONYM:
                return calculateSynonym();
            case AnalyzerStatementTypes.TYPE_DDL_FUNC_HEADER:
            case AnalyzerStatementTypes.TYPE_DDL_FUNC_BODY:
                return calculateFunction(sql);
            case AnalyzerStatementTypes.TYPE_DDL_PROC_HEADER:
            case AnalyzerStatementTypes.TYPE_DDL_PROC_BODY:
                return calculateProcedure(sql);
            case AnalyzerStatementTypes.TYPE_DDL_GRANT:
                return calculateGrant(sql);
            case AnalyzerStatementTypes.TYPE_DDL_PK:
                return calculatePk();
            case AnalyzerStatementTypes.TYPE_DDL_FK:
                return calculateFk(sql);
            case "SELECT":
            case "INSERT":
            case "UPDATE":
            case "DELETE":
                return calculateDml(sql);
            default:
                return 0.0f;
        }
    }

    private float calculateTable(String sql) {
        String normalizedSql = normalizeQuery(sql);
        String upperNormalizedSql = normalizedSql.toUpperCase(Locale.ENGLISH);

        float totalCost = TABLE_BASE_COST;
        totalCost += countCheckConstraints(upperNormalizedSql) * 0.1f;
        totalCost += checkKeyword(upperNormalizedSql, "ENCRYPT") * 20.0f;
        totalCost += calculateKeywordCost(upperNormalizedSql, AnalyzerCostMap.ORA2PG_UNCOVERED_SCORE_MAP, true);
        totalCost += calculateKeywordCost(upperNormalizedSql, AnalyzerCostMap.ORA2PG_ORA_FUNCTION_WEIGHT_MAP, false);
        totalCost += calculateLengthCost(normalizedSql);

        return totalCost;
    }

    private float calculateView(String sql) {
        String normalizedSql = normalizeQuery(sql);
        String upperNormalizedSql = normalizedSql.toUpperCase(Locale.ENGLISH);

        float totalCost = VIEW_BASE_COST;
        if (hasSubquery(upperNormalizedSql)) {
            totalCost += 1.0f;
        }
        if (hasJoin(upperNormalizedSql)) {
            totalCost += 1.0f;
        }
        totalCost += calculateKeywordCost(upperNormalizedSql, AnalyzerCostMap.ORA2PG_UNCOVERED_SCORE_MAP, true);
        totalCost += calculateKeywordCost(upperNormalizedSql, AnalyzerCostMap.ORA2PG_ORA_FUNCTION_WEIGHT_MAP, false);
        totalCost += calculateLengthCost(normalizedSql);

        return totalCost;
    }

    private float calculateIndex(String sql) {
        String upperNormalizedSql = normalizeQuery(sql).toUpperCase(Locale.ENGLISH);

        float totalCost = INDEX_BASE_COST;
        if (isFunctionBasedIndex(upperNormalizedSql)) {
            totalCost += 0.2f;
        }
        if (checkKeyword(upperNormalizedSql, "REVERSE") > 0) {
            totalCost += 1.0f;
        }

        return totalCost;
    }

    private float calculateSequence() {
        return SEQUENCE_BASE_COST;
    }

    private float calculateSynonym() {
        return SYNONYM_BASE_COST;
    }

    private float calculateFunction(String sql) {
        String normalizedSql = normalizeQuery(sql);
        String upperNormalizedSql = normalizedSql.toUpperCase(Locale.ENGLISH);

        float totalCost = FUNCTION_BASE_COST;
        totalCost += calculateKeywordCost(upperNormalizedSql, AnalyzerCostMap.ORA2PG_UNCOVERED_SCORE_MAP, true);
        totalCost += calculateKeywordCost(upperNormalizedSql, AnalyzerCostMap.ORA2PG_ORA_FUNCTION_WEIGHT_MAP, false);
        totalCost += calculateLengthCost(normalizedSql);

        return totalCost;
    }

    private float calculateProcedure(String sql) {
        String normalizedSql = normalizeQuery(sql);
        String upperNormalizedSql = normalizedSql.toUpperCase(Locale.ENGLISH);

        float totalCost = PROCEDURE_BASE_COST;
        totalCost += calculateKeywordCost(upperNormalizedSql, AnalyzerCostMap.ORA2PG_UNCOVERED_SCORE_MAP, true);
        totalCost += calculateKeywordCost(upperNormalizedSql, AnalyzerCostMap.ORA2PG_ORA_FUNCTION_WEIGHT_MAP, false);
        totalCost += calculateLengthCost(normalizedSql);

        return totalCost;
    }

    private float calculateDml(String sql) {
        String normalizedSql = normalizeQuery(sql);
        String upperNormalizedSql = normalizedSql.toUpperCase(Locale.ENGLISH);

        float totalCost = DML_BASE_COST;
        if (hasJoin(upperNormalizedSql)) {
            totalCost += 0.5f;
        }
        if (hasSubquery(upperNormalizedSql)) {
            totalCost += 0.5f;
        }
        totalCost += calculateKeywordCost(upperNormalizedSql, AnalyzerCostMap.ORA2PG_UNCOVERED_SCORE_MAP, true);
        totalCost += calculateKeywordCost(upperNormalizedSql, AnalyzerCostMap.ORA2PG_ORA_FUNCTION_WEIGHT_MAP, false);
        totalCost += calculateLengthCost(normalizedSql);

        return totalCost;
    }

    private float calculateGrant(String sql) {
        String upperNormalizedSql = normalizeQuery(sql).toUpperCase(Locale.ENGLISH);

        float totalCost = GRANT_BASE_COST;
        totalCost += countGrantPrivileges(upperNormalizedSql) * 0.1f;

        if (!hasKeyword(upperNormalizedSql, "ON")) {
            totalCost += 1.0f;
        }
        if (!hasKeyword(upperNormalizedSql, "TO")) {
            totalCost += 1.0f;
        }

        return totalCost;
    }

    private float calculatePk() {
        return PK_BASE_COST;
    }

    private float calculateFk(String sql) {
        String upperNormalizedSql = normalizeQuery(sql).toUpperCase(Locale.ENGLISH);

        float totalCost = FK_BASE_COST;
        if (hasKeyword(upperNormalizedSql, "ON DELETE")) {
            totalCost += 0.5f;
        }

        return totalCost;
    }

    private float calculateKeywordCost(
            String upperSql, Map<String, Float> keywordCostMap, boolean normalizeKeyword) {
        float totalCost = 0.0f;

        for (Map.Entry<String, Float> entry : keywordCostMap.entrySet()) {
            String keyword = entry.getKey();
            String keywordToFind = normalizeKeyword ? keyword.toUpperCase(Locale.ENGLISH) : keyword;
            int occurrenceCount = checkKeyword(upperSql, keywordToFind.toUpperCase(Locale.ENGLISH));
            totalCost += occurrenceCount * entry.getValue();
        }

        return totalCost;
    }

    /**
     * Counts CHECK constraints that should contribute to migration cost.
     * Simple nullability checks such as "IS NOT NULL" are ignored because they
     * behave more like column-nullability metadata than complex business rules.
     */
    private int countCheckConstraints(String upperNormalizedSql) {
        int count = 0;
        int fromIndex = 0;

        while ((fromIndex = findKeywordIndex(upperNormalizedSql, "CHECK", fromIndex)) >= 0) {
            int nextOpenParenIndex = upperNormalizedSql.indexOf("(", fromIndex);
            if (nextOpenParenIndex < 0) {
                count++;
                fromIndex += "CHECK".length();
                continue;
            }

            int nextCloseParenIndex = upperNormalizedSql.indexOf(")", nextOpenParenIndex);
            if (nextCloseParenIndex < 0) {
                count++;
                fromIndex += "CHECK".length();
                continue;
            }

            String condition = upperNormalizedSql.substring(nextOpenParenIndex, nextCloseParenIndex + 1);
            if (!hasKeyword(condition, "IS NOT NULL")) {
                count++;
            }

            fromIndex = nextCloseParenIndex + 1;
        }

        return count;
    }

    /**
     * Counts how many privileges appear between GRANT and ON.
     * Example: "GRANT SELECT, INSERT ON T1 TO U1" returns 2.
     */
    private int countGrantPrivileges(String upperNormalizedSql) {
        int onIndex = findKeywordIndex(upperNormalizedSql, "ON");
        if (!upperNormalizedSql.startsWith("GRANT ") || onIndex < 0) {
            return 0;
        }

        String privilegeSection = upperNormalizedSql.substring("GRANT ".length(), onIndex).trim();
        if (privilegeSection.isEmpty()) {
            return 0;
        }

        if (privilegeSection.contains(",")) {
            return privilegeSection.split("\\s*,\\s*").length;
        }

        return 1;
    }

    private boolean hasJoin(String upperNormalizedSql) {
        return hasKeyword(upperNormalizedSql, "JOIN");
    }

    private boolean hasSubquery(String upperNormalizedSql) {
        return hasPattern(upperNormalizedSql, "\\(\\s*SELECT\\b");
    }

    /**
     * Detects a function-based index by locating the ON clause and then checking
     * whether the indexed column list contains nested parentheses such as
     * "UPPER(NAME)".
     */
    private boolean isFunctionBasedIndex(String upperNormalizedSql) {
        int onIndex = findKeywordIndex(upperNormalizedSql, "ON");
        int openParenIndex = upperNormalizedSql.indexOf("(", onIndex);
        int closeParenIndex = upperNormalizedSql.lastIndexOf(")");
        if (onIndex < 0 || openParenIndex < 0) {
            return false;
        }
        if (closeParenIndex <= openParenIndex) {
            return false;
        }

        String indexColumns = upperNormalizedSql.substring(openParenIndex + 1, closeParenIndex);
        return indexColumns.contains("(");
    }

    /**
     * Counts how many times a keyword or multi-word keyword appears in the text.
     * The generated regex enforces token boundaries, so "ON" is not counted
     * inside "ONLY".
     */
    private int checkKeyword(String text, String keyword) {
        if (text == null || keyword == null || keyword.isEmpty()) {
            return 0;
        }

        return countPatternMatches(text, buildKeywordPattern(keyword));
    }

    /** Convenience wrapper for boolean-style keyword checks. */
    private boolean hasKeyword(String text, String keyword) {
        return checkKeyword(text, keyword) > 0;
    }

    /** Finds the first keyword occurrence starting from the beginning of the text. */
    private int findKeywordIndex(String text, String keyword) {
        return findKeywordIndex(text, keyword, 0);
    }

    /**
     * Finds the first keyword occurrence at or after the given index.
     * This is mainly used when we need a structural anchor such as the ON clause
     * in "CREATE INDEX ... ON ...".
     */
    private int findKeywordIndex(String text, String keyword, int fromIndex) {
        if (text == null || keyword == null || keyword.isEmpty()) {
            return -1;
        }

        Matcher matcher = Pattern.compile(buildKeywordPattern(keyword)).matcher(text);
        return matcher.find(Math.max(fromIndex, 0)) ? matcher.start() : -1;
    }

    /** Generic regex-based existence check for non-keyword structural patterns. */
    private boolean hasPattern(String text, String regex) {
        if (text == null || regex == null || regex.isEmpty()) {
            return false;
        }
        return Pattern.compile(regex).matcher(text).find();
    }

    /** Counts regex matches for patterns that are easier to express directly. */
    private int countPatternMatches(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * Builds a regex for a keyword or multi-word keyword with token boundaries.
     * Internal whitespace becomes "\\s+" so the same keyword still matches when
     * the SQL uses spaces, tabs, or line breaks between words.
     */
    private String buildKeywordPattern(String keyword) {
        String normalizedKeyword = normalizeKeywordPattern(keyword);
        StringBuilder pattern = new StringBuilder();
        if (startsWithWordChar(normalizedKeyword)) {
            pattern.append("(?<![A-Z0-9_])");
        }

        pattern.append(buildLiteralKeywordPattern(normalizedKeyword));

        if (endsWithWordChar(normalizedKeyword) && !normalizedKeyword.endsWith("_")) {
            pattern.append("(?![A-Z0-9_])");
        }

        return pattern.toString();
    }

    /** Normalizes the input keyword to a stable single-space representation. */
    private String normalizeKeywordPattern(String keyword) {
        return keyword.trim().replaceAll("\\s+", " ");
    }

    /**
     * Converts a normalized keyword into a regex-safe literal body.
     * Example: "ON DELETE" becomes "ON\\s+DELETE".
     */
    private String buildLiteralKeywordPattern(String keyword) {
        String[] parts = keyword.split("\\s+");
        StringBuilder pattern = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                pattern.append("\\s+");
            }
            pattern.append(Pattern.quote(parts[i]));
        }
        return pattern.toString();
    }

    /** Checks whether the first character participates in token-boundary rules. */
    private boolean startsWithWordChar(String keyword) {
        return !keyword.isEmpty() && isWordChar(keyword.charAt(0));
    }

    /** Checks whether the last character participates in token-boundary rules. */
    private boolean endsWithWordChar(String keyword) {
        return !keyword.isEmpty() && isWordChar(keyword.charAt(keyword.length() - 1));
    }

    /** Defines what this class treats as a word character inside SQL tokens. */
    private boolean isWordChar(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_';
    }

    /**
     * Collapses formatting differences so the later checks do not depend on
     * tabs, newlines, or repeated spaces in the original SQL text.
     */
    private String normalizeQuery(String sql) {
        if (sql == null) {
            return "";
        }

        return sql.trim().replaceAll("[\\r\\n\\t]+", " ").replaceAll(" +", " ");
    }

    /** Applies the length-based cost tier after structural and keyword checks. */
    private float calculateLengthCost(String normalizedSql) {
        int length = normalizedSql.length();

        if (length <= 199) {
            return 0.0f;
        }
        if (length <= 499) {
            return 1.0f;
        }
        if (length <= 999) {
            return 2.0f;
        }
        return 4.0f;
    }
}
