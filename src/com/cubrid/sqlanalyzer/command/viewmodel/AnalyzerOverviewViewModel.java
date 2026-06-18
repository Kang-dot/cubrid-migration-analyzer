package com.cubrid.sqlanalyzer.command.viewmodel;

import java.util.List;

import com.cubrid.sqlanalyzer.command.AnalyzerExecutionMode;

public record AnalyzerOverviewViewModel(
        String programVersion,
        List<AnalyzerSourceOverviewViewModel> sources,
        AnalyzerTargetOverviewViewModel target,
        AnalyzerExecutionMode executionMode,
        List<String> sourceStatusMessages) {

    public AnalyzerOverviewViewModel(
            String programVersion,
            AnalyzerSourceOverviewViewModel source,
            AnalyzerTargetOverviewViewModel target,
            AnalyzerExecutionMode executionMode) {
        this(
                programVersion,
                source == null ? List.of() : List.of(source),
                target,
                executionMode,
                List.of());
    }

    public AnalyzerOverviewViewModel {
        sources = sources == null ? List.of() : List.copyOf(sources);
        sourceStatusMessages =
                sourceStatusMessages == null ? List.of() : List.copyOf(sourceStatusMessages);
    }

    public AnalyzerSourceOverviewViewModel source() {
        return sources.isEmpty() ? null : sources.get(0);
    }
}
