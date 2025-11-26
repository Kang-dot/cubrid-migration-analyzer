package com.cubrid.sqlanalyzer.ui.page.service;

import com.cubrid.sqlanalyzer.ui.AnalyzerWizard;

/**
 * Interface responsible for text generation logic in Analyzer confirmation page.
 * Separated as interface for testing convenience.
 */
public interface IAnalyzerConfirmTextBuilder {

	/**
	 * Builds a complete confirmation text to be rendered on the page.
	 *
	 * @param wizard AnalyzerWizard
	 * @return rendered confirmation text
	 */
	String buildText(AnalyzerWizard wizard);
}

