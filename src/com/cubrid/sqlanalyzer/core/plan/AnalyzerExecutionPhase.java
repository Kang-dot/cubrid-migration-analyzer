package com.cubrid.sqlanalyzer.core.plan;

public enum AnalyzerExecutionPhase {
    DDL_TABLE,
    DDL_PRIMARY_KEY,
    DDL_FOREIGN_KEY,
    DDL_INDEX,
    DDL_SEQUENCE,
    DDL_VIEW_CREATE,
    DDL_VIEW_ALTER,
    DDL_SYNONYM,
    DDL_GRANT,
    DDL_PROCEDURE_HEADER,
    DDL_PROCEDURE_BODY,
    DDL_FUNCTION_HEADER,
    DDL_FUNCTION_BODY,
    DDL_TRIGGER,
    DML_SELECT,
    DML_INSERT,
    DML_UPDATE,
    DML_DELETE
}
