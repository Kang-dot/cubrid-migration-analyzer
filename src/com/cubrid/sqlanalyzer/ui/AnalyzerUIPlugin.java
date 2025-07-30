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

		// XML 파싱을 시작하는 메소드 호출
		AnalyzerConnectionManager connectionManager = new AnalyzerConnectionManager();

		try {
			if (file.exists()) {
				// 파일이 존재하면 XML 파싱 시작
				connectionManager.loadConnectionData(file);
			} else {
				// 파일이 없으면 새로 생성하거나 기본 설정 로드
				file.createNewFile();
				// 필요시 기본 연결 정보를 추가하고 파일로 저장
				// connectionManager.saveConnectionsToFile(file);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
