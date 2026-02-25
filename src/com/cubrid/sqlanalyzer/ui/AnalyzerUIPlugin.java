package com.cubrid.sqlanalyzer.ui;

import java.io.File;

import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.cubrid.sqlanalyzer.core.AnalyzerConnectionManager;

public class AnalyzerUIPlugin extends AbstractUIPlugin {
	public AnalyzerUIPlugin() {
		initConnectionLocation();
	}

	public void initConnectionLocation() {
		File file = this.getStateLocation().append("analyzerconnection.xml").toFile();

		AnalyzerConnectionManager connectionManager = new AnalyzerConnectionManager();

		try {
			if (file.exists()) {
				connectionManager.loadConnectionData(file);
			} else {
				file.createNewFile();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
