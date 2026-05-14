package com.cubrid.sqlanalyzer.command;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

public class AnalyzerConsoleArguments {
    private static final String DEFAULT_XML_CHARSET = "UTF-8";

    private boolean interactive = true;

    @Parameter(names = "-jr", description = "Optional JDBC driver repository directory")
    private String jdbcRepositoryDir;

    private AnalyzerSourceType sourceType;
    private AnalyzerTargetType targetType;
    private String sourceJdbcUrl;
    private String sourceUser;
    private String sourcePassword;
    private String xmlDirectory;
    private String xmlCharset = DEFAULT_XML_CHARSET;
    private String targetJdbcUrl;
    private String targetUser;
    private String targetPassword;

    /** Buffer for arguments without an option name, e.g. legacy JDBC repository directory. */
    @Parameter(description = "jdbcRepositoryDir")
    private List<String> positionalArgumentBuffer = new ArrayList<String>();

    @Parameter(names = "-so", description = "Source is Oracle JDBC")
    private boolean oracleSource;

    @Parameter(names = "-sx", description = "Source is XML directory")
    private boolean xmlSource;

    @Parameter(names = "-oj", description = "Oracle connection spec: <jdbcUrl|user|password>")
    private String oracleConnectionSpec;

    @Parameter(names = "-xd", description = "XML directory path")
    private String parsedXmlDirectory;

    @Parameter(names = "-xc", description = "XML charset. Default: UTF-8")
    private String parsedXmlCharset = DEFAULT_XML_CHARSET;

    @Parameter(names = "-tp", description = "Target is embedded parser")
    private boolean parserTarget;

    @Parameter(names = "-tc", description = "Target is CUBRID JDBC")
    private boolean jdbcTarget;

    @Parameter(names = "-cj", description = "CUBRID connection spec: <jdbcUrl|user|password>")
    private String cubridConnectionSpec;

    public static AnalyzerConsoleArguments parse(String[] args) {
        AnalyzerConsoleArguments arguments = new AnalyzerConsoleArguments();
        if (args == null || args.length == 0) {
            return arguments;
        }

        JCommander commander = newCommander(arguments);
        try {
            commander.parse(args);
        } catch (ParameterException ex) {
            throw new IllegalArgumentException(ex.getMessage() + System.lineSeparator() + usage());
        }

        arguments.applyParsedValues(args);
        if (arguments.isInteractive()) {
            return arguments;
        }
        arguments.validate();
        return arguments;
    }

    private static JCommander newCommander(AnalyzerConsoleArguments arguments) {
        return JCommander.newBuilder()
                .addObject(arguments)
                .programName("java -jar analyzer.jar")
                .build();
    }

    public static String usage() {
        return "Usage:" + System.lineSeparator()
                + "  java -jar analyzer.jar -so -oj <jdbcUrl|user|password> -tp"
                + System.lineSeparator()
                + "  java -jar analyzer.jar -so -oj <jdbcUrl|user|password> -tc -cj <jdbcUrl|user|password>"
                + System.lineSeparator()
                + "  java -jar analyzer.jar -sx -xd <xmlDirectory> -tp"
                + System.lineSeparator()
                + "  java -jar analyzer.jar -sx -xd <xmlDirectory> -tc -cj <jdbcUrl|user|password>"
                + System.lineSeparator()
                + "  java -jar analyzer.jar -conf <settingsFile>"
                + System.lineSeparator()
                + System.lineSeparator()
                + "Options:" + System.lineSeparator()
                + "  -conf <path> Settings file path. Default: settings/setting.conf"
                + System.lineSeparator()
                + "  -jr <path>   Optional JDBC driver repository directory"
                + System.lineSeparator()
                + "  -so          Source is Oracle JDBC" + System.lineSeparator()
                + "  -oj <spec>   Oracle connection spec: <jdbcUrl|user|password>"
                + System.lineSeparator()
                + "  -sx          Source is XML directory" + System.lineSeparator()
                + "  -xd <path>   XML directory path" + System.lineSeparator()
                + "  -xc <name>   XML charset. Default: UTF-8" + System.lineSeparator()
                + "  -tp          Target is embedded parser" + System.lineSeparator()
                + "  -tc          Target is CUBRID JDBC" + System.lineSeparator()
                + "  -cj <spec>   CUBRID connection spec: <jdbcUrl|user|password>";
    }

    public boolean isInteractive() {
        return interactive;
    }

    public String getJdbcRepositoryDir() {
        return jdbcRepositoryDir;
    }

    public AnalyzerSourceType getSourceType() {
        return sourceType;
    }

    public AnalyzerTargetType getTargetType() {
        return targetType;
    }

    public String getSourceJdbcUrl() {
        return sourceJdbcUrl;
    }

    public String getSourceUser() {
        return sourceUser;
    }

    public String getSourcePassword() {
        return sourcePassword;
    }

    public String getXmlDirectory() {
        return xmlDirectory;
    }

    public String getXmlCharset() {
        return xmlCharset;
    }

    public String getTargetJdbcUrl() {
        return targetJdbcUrl;
    }

    public String getTargetUser() {
        return targetUser;
    }

    public String getTargetPassword() {
        return targetPassword;
    }

    private void applyParsedValues(String[] args) {
        if (positionalArgumentBuffer.size() > 1) {
            throw new IllegalArgumentException(
                    "Only one JDBC repository directory argument is allowed."
                            + System.lineSeparator()
                            + usage());
        }

        if (!positionalArgumentBuffer.isEmpty()) {
            jdbcRepositoryDir = positionalArgumentBuffer.get(0);
        }

        if (args.length == 1 && !args[0].startsWith("-")) {
            interactive = true;
            return;
        }

        interactive = false;
        if (countOptions(args, "-so", "-sx") > 1) {
            throw new IllegalArgumentException(
                    "Only one source option is allowed." + System.lineSeparator() + usage());
        }
        if (countOptions(args, "-tp", "-tc") > 1) {
            throw new IllegalArgumentException(
                    "Only one target option is allowed." + System.lineSeparator() + usage());
        }

        if (oracleSource) {
            sourceType = AnalyzerSourceType.ORACLE;
        } else if (xmlSource) {
            sourceType = AnalyzerSourceType.XML;
        }

        if (parserTarget) {
            targetType = AnalyzerTargetType.PARSER;
        } else if (jdbcTarget) {
            targetType = AnalyzerTargetType.JDBC;
        }

        if (oracleConnectionSpec != null) {
            String[] values = splitConnectionSpec(oracleConnectionSpec, "-oj");
            sourceJdbcUrl = values[0];
            sourceUser = values[1];
            sourcePassword = values[2];
        }

        xmlDirectory = parsedXmlDirectory;
        xmlCharset = parsedXmlCharset;

        if (cubridConnectionSpec != null) {
            String[] values = splitConnectionSpec(cubridConnectionSpec, "-cj");
            targetJdbcUrl = values[0];
            targetUser = values[1];
            targetPassword = values[2];
        }
    }

    private void validate() {
        if (sourceType == null) {
            throw new IllegalArgumentException("Source option is required." + System.lineSeparator() + usage());
        }
        if (targetType == null) {
            throw new IllegalArgumentException("Target option is required." + System.lineSeparator() + usage());
        }

        if (AnalyzerSourceType.ORACLE.equals(sourceType)) {
            if (sourceJdbcUrl == null || sourceUser == null || sourcePassword == null) {
                throw new IllegalArgumentException(
                        "-so requires -oj <jdbcUrl|user|password>." + System.lineSeparator() + usage());
            }
        } else if (AnalyzerSourceType.XML.equals(sourceType)) {
            if (xmlDirectory == null || xmlDirectory.isEmpty()) {
                throw new IllegalArgumentException(
                        "-sx requires -xd <xmlDirectory>." + System.lineSeparator() + usage());
            }
        }

        if (AnalyzerTargetType.JDBC.equals(targetType)
                && (targetJdbcUrl == null || targetUser == null || targetPassword == null)) {
            throw new IllegalArgumentException(
                    "-tc requires -cj <jdbcUrl|user|password>." + System.lineSeparator() + usage());
        }
    }

    private static int countOptions(String[] args, String firstOption, String secondOption) {
        int count = 0;
        for (String arg : args) {
            if (firstOption.equals(arg) || secondOption.equals(arg)) {
                count++;
            }
        }
        return count;
    }

    private static String[] splitConnectionSpec(String spec, String option) {
        String[] values = spec.split("\\|", 3);
        if (values.length != 3) {
            values = spec.split(",", 3);
        }
        if (values.length != 3) {
            throw new IllegalArgumentException(
                    option + " must be <jdbcUrl|user|password>." + System.lineSeparator() + usage());
        }
        if (values[0].isEmpty() || values[1].isEmpty()) {
            throw new IllegalArgumentException(
                    option + " must include jdbcUrl and user." + System.lineSeparator() + usage());
        }
        return values;
    }
}
