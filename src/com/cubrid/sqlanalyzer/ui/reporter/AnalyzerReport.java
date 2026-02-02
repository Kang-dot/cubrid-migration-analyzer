package com.cubrid.sqlanalyzer.ui.reporter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Analyzer Report model - Minimal implementation that stores only query execution results
 * 
 * @author Generated
 */
public class AnalyzerReport implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Internal class that stores query execution results
	 */

	private List<AnalyzerOverviewResult> selectResultList = new ArrayList<>();
	private List<AnalyzerOverviewResult> insertResultList = new ArrayList<>();
	private List<AnalyzerOverviewResult> deleteResultList = new ArrayList<>();
	private List<AnalyzerOverviewResult> updateResultList = new ArrayList<>();
	
	private long selectTotalCount;
	private long insertTotalCount;
	private long deleteTotalCount;
	private long updateTotalCount;

	private long totalStartTime;
	private long totalEndTime;
	private final List<AnalyzerOverviewResult> queryResults = new ArrayList<AnalyzerOverviewResult>();

	/**
	 * Add query execution result
	 * 
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

		switch (queryType) {
		case "SELECT":
			selectResultList.add(result);
			selectTotalCount += 1;
			break;
		case "INSERT":
			insertResultList.add(result);
			insertTotalCount += 1;
			break;
		case "DELETE":
			deleteResultList.add(result);
			deleteTotalCount += 1;
			break;
		case "UPDATE":
			updateResultList.add(result);
			updateTotalCount += 1;
			break;
		}
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
		return selectTotalCount;
	}

	public long getInsertTotalCount() {
		return insertTotalCount;
	}

	public long getDeleteTotalCount() {
		return deleteTotalCount;
	}

	public long getUpdateTotalCount() {
		return updateTotalCount;
	}

	public List<AnalyzerOverviewResult> getOverviewResults() {
		selectResultList.get(0).setQueryType("SELECT");
		selectResultList.get(0).setTotalCount(selectTotalCount);
		
		insertResultList.get(0).setQueryType("INSERT");
		insertResultList.get(0).setTotalCount(insertTotalCount);

		deleteResultList.get(0).setQueryType("DELETE");
		deleteResultList.get(0).setTotalCount(deleteTotalCount);

		updateResultList.get(0).setQueryType("UPDATE");
		updateResultList.get(0).setTotalCount(updateTotalCount);

		ArrayList<AnalyzerOverviewResult> totalResultList = new ArrayList<>();
		
		totalResultList.addAll(selectResultList);
		totalResultList.addAll(insertResultList);
		totalResultList.addAll(deleteResultList);
		totalResultList.addAll(updateResultList);

		return totalResultList;
	}
}
