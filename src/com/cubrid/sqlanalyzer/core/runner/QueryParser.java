package com.cubrid.sqlanalyzer.core.runner;

public class QueryParser {
	
	static {
		try {
			String osName = System.getProperty("os.name").toLowerCase();
			if (osName.contains("win") || osName.contains("linux")) {
				System.loadLibrary("sqlvalidator");
			}
		} catch (UnsatisfiedLinkError e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * JNI native method to validate SQL query using C library.
	 * 
	 * @param query SQL string to validate
	 * @return "NO_ERROR" if valid, or error message if invalid
	 */
	public native String validateSQL(String query);

	/**
	 * Check SQL query using validateSQL and throw SQLAnalyzeException if invalid.
	 * 
	 * @param query SQL string to validate
	 * @throws SQLParserException if validation fails
	 */
	public void checkSQL(String query) throws SQLParserException {
		String result = validateSQL(query);
		if (!"NO_ERROR".equals(result)) {
			throw new SQLParserException(result);
		}
	}
}
