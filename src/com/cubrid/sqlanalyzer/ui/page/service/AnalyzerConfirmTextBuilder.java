package com.cubrid.sqlanalyzer.ui.page.service;

import com.cubrid.sqlanalyzer.core.dbobject.AnalyzerCatalog;
import com.cubrid.sqlanalyzer.core.dbobject.QueryDictionary;
import com.cubrid.sqlanalyzer.ui.AnalyzerWizard;

/**
 * Service class responsible for text generation logic in Analyzer confirmation page.
 * Composed of pure Java logic separated from view for testing convenience.
 */
public class AnalyzerConfirmTextBuilder implements IAnalyzerConfirmTextBuilder {
	private final String tabSeparator = "\t";
	private final String lineSeparator = System.getProperty("line.separator");
	
	
	/**
	 * Builds the full confirmation text.
	 *
	 * @param wizard AnalyzerWizard
	 * @return rendered confirmation text
	 */
	public String buildText(AnalyzerWizard wizard) {
		if (wizard == null) {
			return "";
		}
		
		AnalyzerCatalog analyzerCatalog = (AnalyzerCatalog) wizard.getOriginalSourceCatalog();
		if (analyzerCatalog == null) {
			return "";
		}
		
		QueryDictionary queryDict = analyzerCatalog.getQueryDictionary();
		if (queryDict == null) {
			return "";
		}
		
		StringBuilder sb = new StringBuilder();
		appendConfigurationInfo(sb, queryDict);
		sb.append(lineSeparator).append(lineSeparator);
		appendDDLInfo(sb, queryDict);
		return sb.toString();
	}
	
	/**
	 * Appends configuration information to StringBuilder.
	 *
	 * @param sb StringBuilder
	 * @param dict QueryDictionary
	 */
	protected void appendConfigurationInfo(StringBuilder sb, QueryDictionary dict) {
		// TODO: Implement logic to convert actual configuration information to string
		sb.append("=== DML List ===\n\n");
		sb.append(tabSeparator);
		sb.append("select query: " + dict.getSelectQueryMap().size());
        sb.append(lineSeparator);
        sb.append(tabSeparator);
        sb.append("insert query: " + dict.getInsertQueryMap().size());
        sb.append(lineSeparator);
        sb.append(tabSeparator);
        sb.append("update query: " + dict.getUpdateQueryMap().size());
        sb.append(lineSeparator);
        sb.append(tabSeparator);
        sb.append("delete query: " + dict.getDeleteQueryMap().size());
        sb.append(lineSeparator);
	}

	/**
	 * Appends DDL information to StringBuilder.
	 *
	 * @param sb StringBuilder
	 * @param dict QueryDictionary
	 */
	protected void appendDDLInfo(StringBuilder sb, QueryDictionary dict) {
		// TODO: Implement actual DDL generation logic
        sb.append("=== DDL List ===\n\n");
        sb.append(tabSeparator);
        sb.append("DDL query: TODO");
	}
}

