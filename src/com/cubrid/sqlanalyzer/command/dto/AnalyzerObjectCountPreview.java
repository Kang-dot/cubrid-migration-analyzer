package com.cubrid.sqlanalyzer.command.dto;

import com.cubrid.sqlanalyzer.command.AnalyzerSourceType;

public record AnalyzerObjectCountPreview(
        AnalyzerSourceType sourceType,
        int catalogSchemaCount,
        int targetTableCount,
        long targetPkCount,
        long targetFkCount,
        int targetViewCount,
        int targetSerialCount,
        int targetSynonymCount,
        int targetGrantCount,
        int targetProcedureCount,
        int targetFunctionCount,
        int selectCount,
        int insertCount,
        int updateCount,
        int deleteCount) {
}
