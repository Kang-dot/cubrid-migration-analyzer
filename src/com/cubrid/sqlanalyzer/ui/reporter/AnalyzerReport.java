package com.cubrid.sqlanalyzer.ui.reporter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Analyzer Report model - manages statement categories efficiently
 * 
 * @author Generated
 */
public class AnalyzerReport implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Internal class to encapsulate results and statistics for each statement type
	 */
	private static class DMLCategory implements Serializable {
		private static final long serialVersionUID = 1L;
		private final String typeName;
		private final List<AnalyzerOverviewResult> results = new ArrayList<>();
		private long totalCount = 0;
		private long errorCount = 0;

		DMLCategory(String typeName) {
			this.typeName = typeName;
		}

		void addResult(AnalyzerOverviewResult result) {
			results.add(result);
			totalCount++;
			if (!result.isSuccess()) {
				errorCount++;
			}
		}
	}

	private final Map<String, DMLCategory> categoryMap = new LinkedHashMap<>();
	private final List<AnalyzerOverviewResult> queryResults = new ArrayList<>();
	private long totalStartTime;
	private long totalEndTime;

	public AnalyzerReport() {
		// Initialize categories in display order
		categoryMap.put("DDL_TABLE", new DMLCategory("DDL_TABLE"));
		categoryMap.put("DDL_VIEW", new DMLCategory("DDL_VIEW"));
		categoryMap.put("SELECT", new DMLCategory("SELECT"));
		categoryMap.put("INSERT", new DMLCategory("INSERT"));
		categoryMap.put("DELETE", new DMLCategory("DELETE"));
		categoryMap.put("UPDATE", new DMLCategory("UPDATE"));
	}

	/**
	 * Add query execution result
	 * 
	 * @param queryType    DML type (SELECT, INSERT, etc.)
	 * @param queryId      Query ID
	 * @param query        Query content
	 * @param success      Success status
	 * @param errorMessage Error message (on failure)
	 * @param executeTime  Execution time
	 */
	public void addQueryResult(String queryType, String queryId, String query, boolean success, String errorMessage,
			long executeTime) {
		AnalyzerOverviewResult result = new AnalyzerOverviewResult(queryId, query, success, errorMessage,
				executeTime);
		queryResults.add(result);

		DMLCategory category = getOrCreateCategory(queryType);
		category.addResult(result);
	}

	/**
	 * Return all query execution results
	 * 
	 * @return List of query execution results
	 */
	public List<AnalyzerOverviewResult> getQueryResults() {
		return new ArrayList<AnalyzerOverviewResult>(queryResults);
	}

	/**
	 * Number of successful queries
	 * 
	 * @return Success count
	 */
	public int getSuccessCount() {
		int count = 0;
		for (AnalyzerOverviewResult result : queryResults) {
			if (result.isSuccess()) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Number of failed queries
	 * 
	 * @return Failure count
	 */
	public int getFailedCount() {
		int count = 0;
		for (AnalyzerOverviewResult result : queryResults) {
			if (!result.isSuccess()) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Total number of queries
	 * 
	 * @return Total count
	 */
	public int getTotalCount() {
		return queryResults.size();
	}

	/**
	 * Check if there are errors
	 * 
	 * @return Whether errors exist
	 */
	public boolean hasError() {
		return getFailedCount() > 0;
	}

	public long getTotalStartTime() {
		return totalStartTime;
	}

	public void setTotalStartTime(long totalStartTime) {
		this.totalStartTime = totalStartTime;
	}

	public long getTotalEndTime() {
		return totalEndTime;
	}

	public void setTotalEndTime(long totalEndTime) {
		this.totalEndTime = totalEndTime;
	}

	public long getSelectTotalCount() {
		return getCategoryTotalCount("SELECT");
	}

	public long getTableTotalCount() {
		return getCategoryTotalCount("DDL_TABLE");
	}

	public long getViewTotalCount() {
		return getCategoryTotalCount("DDL_VIEW");
	}

	public long getInsertTotalCount() {
		return getCategoryTotalCount("INSERT");
	}

	public long getDeleteTotalCount() {
		return getCategoryTotalCount("DELETE");
	}

	public long getUpdateTotalCount() {
		return getCategoryTotalCount("UPDATE");
	}
	
	public long getSelectErrorCount() {
		return getCategoryErrorCount("SELECT");
	}

	public long getTableErrorCount() {
		return getCategoryErrorCount("DDL_TABLE");
	}

	public long getViewErrorCount() {
		return getCategoryErrorCount("DDL_VIEW");
	}

	public long getInsertErrorCount() {
		return getCategoryErrorCount("INSERT");
	}

	public long getDeleteErrorCount() {
		return getCategoryErrorCount("DELETE");
	}

	public long getUpdateErrorCount() {
		return getCategoryErrorCount("UPDATE");
	}

	public List<AnalyzerOverviewResult> getSelectResults() {
		return getCategoryResults("SELECT");
	}

	public List<AnalyzerOverviewResult> getTableResults() {
		return getCategoryResults("DDL_TABLE");
	}

	public List<AnalyzerOverviewResult> getViewResults() {
		return getCategoryResults("DDL_VIEW");
	}
	
	public List<AnalyzerOverviewResult> getInsertResults() {
		return getCategoryResults("INSERT");
	}
	
	public List<AnalyzerOverviewResult> getDeleteResults() {
		return getCategoryResults("DELETE");
	}

	public List<AnalyzerOverviewResult> getUpdateResults() {
		return getCategoryResults("UPDATE");
	}

	/**
	 * Aggregates all category results for overview
	 * 
	 * @return Combined list of results
	 */
	public List<AnalyzerOverviewResult> getOverviewResults() {
		ArrayList<AnalyzerOverviewResult> totalResultList = new ArrayList<>();
		for (DMLCategory category : categoryMap.values()) {
			if (!category.results.isEmpty()) {
				// Set grouping metadata on the first item of each category
				AnalyzerOverviewResult first = category.results.get(0);
				first.setQueryType(category.typeName);
				first.setTotalCount(category.totalCount);
			}
			totalResultList.addAll(category.results);
		}
		return totalResultList;
	}

	public List<String> getCategoryTypes() {
		return new ArrayList<String>(categoryMap.keySet());
	}

	public List<AnalyzerOverviewResult> getResultsByType(String type) {
		return new ArrayList<AnalyzerOverviewResult>(getCategoryResults(type));
	}

	public long getTotalCountByType(String type) {
		return getCategoryTotalCount(type);
	}

	public long getErrorCountByType(String type) {
		return getCategoryErrorCount(type);
	}

	private DMLCategory getOrCreateCategory(String type) {
		String normalizedType = normalizeType(type);
		DMLCategory category = categoryMap.get(normalizedType);
		if (category == null) {
			category = new DMLCategory(normalizedType);
			categoryMap.put(normalizedType, category);
		}
		return category;
	}

	private long getCategoryTotalCount(String type) {
		DMLCategory cat = categoryMap.get(normalizeType(type));
		return cat != null ? cat.totalCount : 0;
	}

	private long getCategoryErrorCount(String type) {
		DMLCategory cat = categoryMap.get(normalizeType(type));
		return cat != null ? cat.errorCount : 0;
	}

	private List<AnalyzerOverviewResult> getCategoryResults(String type) {
		DMLCategory cat = categoryMap.get(normalizeType(type));
		return cat != null ? cat.results : new ArrayList<>();
	}

	private String normalizeType(String type) {
		if (type == null) {
			return "UNKNOWN";
		}

		String trimmedType = type.trim();
		return trimmedType.isEmpty() ? "UNKNOWN" : trimmedType;
	}
}
