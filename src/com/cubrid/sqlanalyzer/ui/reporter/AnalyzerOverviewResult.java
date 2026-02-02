package com.cubrid.sqlanalyzer.ui.reporter;

import java.io.Serializable;

public class AnalyzerOverviewResult {
	private static final long serialVersionUID = 1L;

	private String queryType;
	private long totalCount;
	
	private String queryId;
	private String query;
	private boolean success;
	private String errorMessage;
	private long executeTime;

	public AnalyzerOverviewResult(String queryId, String query, boolean success, String errorMessage,
			long executeTime) {
		this.queryType = "";
		this.totalCount = 0;
		this.queryId = queryId;
		this.query = query;
		this.success = success;
		this.errorMessage = errorMessage;
		this.executeTime = executeTime;
	}

	
	public AnalyzerOverviewResult(String queryType, long totalCount, String queryId, String query, boolean success, String errorMessage,
			long executeTime) {
		this.queryType = queryType;
		this.totalCount = totalCount;
		this.queryId = queryId;
		this.query = query;
		this.success = success;
		this.errorMessage = errorMessage;
		this.executeTime = executeTime;
	}

	public String getQueryType() {
		return queryType;
	}
	
	public void setQueryType(String queryType) {
		this.queryType = queryType;
	}
	
	public long getTotalCount() {
		return totalCount;
	}
	
	public void setTotalCount(long totalCount) {
		this.totalCount = totalCount;
	}
	
	public String getQueryId() {
		return queryId;
	}

	public void setQueryId(String queryId) {
		this.queryId = queryId;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public long getExecuteTime() {
		return executeTime;
	}

	public void setExecuteTime(long executeTime) {
		this.executeTime = executeTime;
	}
}
