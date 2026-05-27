package com.cubrid.sqlanalyzer.command.viewmodel;

import com.cubrid.sqlanalyzer.command.AnalyzerSourceType;

public record AnalyzerObjectCountPreviewViewModel(
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
        int targetTriggerCount,
        int selectCount,
        int insertCount,
        int updateCount,
        int deleteCount) {
}
