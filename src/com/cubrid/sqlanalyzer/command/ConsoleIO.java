package com.cubrid.sqlanalyzer.command;

public interface ConsoleIO {
    void print(String text);

    void println(String text);

    String readLine();

    String readRequired(String prompt);

    boolean confirm(String prompt);
}
