package com.cubrid.sqlanalyzer.command.viewmodel;

import java.util.List;

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
        int deleteCount,
        long totalTableBytes,
        List<AnalyzerTableSizeViewModel> tableSizes) {

    public AnalyzerObjectCountPreviewViewModel(
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
        this(
                sourceType,
                catalogSchemaCount,
                targetTableCount,
                targetPkCount,
                targetFkCount,
                targetViewCount,
                targetSerialCount,
                targetSynonymCount,
                targetGrantCount,
                targetProcedureCount,
                targetFunctionCount,
                targetTriggerCount,
                selectCount,
                insertCount,
                updateCount,
                deleteCount,
                0,
                List.of());
    }

    public AnalyzerObjectCountPreviewViewModel {
        tableSizes = tableSizes == null ? List.of() : List.copyOf(tableSizes);
    }
}
