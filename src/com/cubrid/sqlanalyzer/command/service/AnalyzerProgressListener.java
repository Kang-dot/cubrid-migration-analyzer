package com.cubrid.sqlanalyzer.command.service;

import com.cubrid.sqlanalyzer.command.dto.AnalyzerProgressEvent;

public interface AnalyzerProgressListener {
    void onProgress(AnalyzerProgressEvent event);
}
