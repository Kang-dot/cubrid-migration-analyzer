package com.cubrid.sqlanalyzer.core.runner;

import com.cubrid.plcsql.compiler.PlcsqlCompilerMain;
import com.cubrid.plcsql.compiler.error.SyntaxError;

public class PlcsqlChecker {
    private static final int PLCSQL_SYNTAX_ERROR_CODE = -1;

    public void checkSQL(String query) throws SQLParserException {
        try {
            PlcsqlCompilerMain.checkSyntax(query);
        } catch (SyntaxError e) {
            throw new SQLParserException(formatSyntaxError(e), e);
        } catch (RuntimeException e) {
            throw new SQLParserException("PL/CSQL checker failed.", e);
        }
    }

    private String formatSyntaxError(SyntaxError error) {
        String message = error.getMessage();
        if (message == null) {
            message = "PL/CSQL SyntaxError message is null.";
        } else if (message.isBlank()) {
            message = "PL/CSQL SyntaxError message is blank.";
        }

        return String.format(
                "In line %d, column %d,%n%nERROR(%d): %s",
                error.line,
                error.column,
                PLCSQL_SYNTAX_ERROR_CODE,
                message);
    }
}
