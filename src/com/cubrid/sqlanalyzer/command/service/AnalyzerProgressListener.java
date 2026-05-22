package com.cubrid.sqlanalyzer.command.service;

import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressEventViewModel;

public interface AnalyzerProgressListener {
    void onProgress(AnalyzerProgressEventViewModel event);
}
