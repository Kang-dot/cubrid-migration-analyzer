package com.cubrid.sqlanalyzer.core.cost;

import com.cubrid.sqlanalyzer.command.AnalyzerConsoleFailure;
import com.cubrid.sqlanalyzer.command.AnalyzerConsoleReport;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes;

import java.util.Locale;
import java.util.Map;

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
        if (upperNormalizedSql.contains(" REVERSE ")) {
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

        if (!upperNormalizedSql.contains(" ON ")) {
            totalCost += 1.0f;
        }
        if (!upperNormalizedSql.contains(" TO ")) {
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
        if (upperNormalizedSql.contains("ON DELETE")) {
            totalCost += 0.5f;
        }

        return totalCost;
    }

    private float calculateKeywordCost(
            String upperSql, Map<String, Float> keywordCostMap, boolean normalizeKeyword) {
        float totalCost = 0.0f;

        for (Map.Entry<String, Float> entry : keywordCostMap.entrySet()) {
            String keyword = entry.getKey();
            String keywordToFind =
                    normalizeKeyword ? keyword.toUpperCase(Locale.ENGLISH) : keyword;
            int occurrenceCount = checkKeyword(upperSql, keywordToFind.toUpperCase(Locale.ENGLISH));
            totalCost += occurrenceCount * entry.getValue();
        }

        return totalCost;
    }

    private int countCheckConstraints(String upperNormalizedSql) {
        int count = 0;
        int fromIndex = 0;

        while ((fromIndex = upperNormalizedSql.indexOf("CHECK", fromIndex)) >= 0) {
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
            if (!condition.contains("IS NOT NULL")) {
                count++;
            }

            fromIndex = nextCloseParenIndex + 1;
        }

        return count;
    }

    private int countGrantPrivileges(String upperNormalizedSql) {
        int onIndex = upperNormalizedSql.indexOf(" ON ");
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
        return upperNormalizedSql.contains(" JOIN ");
    }

    private boolean hasSubquery(String upperNormalizedSql) {
        return upperNormalizedSql.contains("(SELECT")
                || upperNormalizedSql.contains("( SELECT")
                || upperNormalizedSql.contains("IN (SELECT")
                || upperNormalizedSql.contains("IN ( SELECT")
                || upperNormalizedSql.contains("EXISTS (SELECT")
                || upperNormalizedSql.contains("EXISTS ( SELECT")
                || upperNormalizedSql.contains("FROM (SELECT")
                || upperNormalizedSql.contains("FROM ( SELECT");
    }

    private boolean isFunctionBasedIndex(String upperNormalizedSql) {
        int onIndex = upperNormalizedSql.indexOf(" ON ");
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

    private int checkKeyword(String text, String keyword) {
        if (text == null || keyword == null || keyword.isEmpty()) {
            return 0;
        }

        int count = 0;
        int fromIndex = 0;
        int foundIndex;

        while ((foundIndex = text.indexOf(keyword, fromIndex)) >= 0) {
            count++;
            fromIndex = foundIndex + keyword.length();
        }

        return count;
    }

    private String normalizeQuery(String sql) {
        if (sql == null) {
            return "";
        }

        return sql.trim().replaceAll("[\\r\\n\\t]+", " ").replaceAll(" +", " ");
    }

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
