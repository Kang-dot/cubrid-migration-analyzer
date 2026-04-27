package com.cubrid.sqlanalyzer.command;

public class AnalyzerConsoleArguments {
    private static final String DEFAULT_XML_CHARSET = "UTF-8";

    private boolean interactive = true;
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

    public static AnalyzerConsoleArguments parse(String[] args) {
        AnalyzerConsoleArguments arguments = new AnalyzerConsoleArguments();
        if (args == null || args.length == 0) {
            return arguments;
        }

        int index = 0;
        if (!args[0].startsWith("-")) {
            arguments.jdbcRepositoryDir = args[0];
            index = 1;
        }

        if (index >= args.length) {
            return arguments;
        }

        arguments.interactive = false;

        while (index < args.length) {
            String option = args[index++];
            if ("-jr".equals(option)) {
                arguments.jdbcRepositoryDir = requireValue(args, index++, option);
            } else if ("-so".equals(option)) {
                arguments.ensureSourceTypeUnset(option);
                arguments.sourceType = AnalyzerSourceType.ORACLE;
            } else if ("-sx".equals(option)) {
                arguments.ensureSourceTypeUnset(option);
                arguments.sourceType = AnalyzerSourceType.XML;
            } else if ("-oj".equals(option)) {
                String[] values = splitConnectionSpec(requireValue(args, index++, option), option);
                arguments.sourceJdbcUrl = values[0];
                arguments.sourceUser = values[1];
                arguments.sourcePassword = values[2];
            } else if ("-xd".equals(option)) {
                arguments.xmlDirectory = requireValue(args, index++, option);
            } else if ("-tp".equals(option)) {
                arguments.ensureTargetTypeUnset(option);
                arguments.targetType = AnalyzerTargetType.PARSER;
            } else if ("-tc".equals(option)) {
                arguments.ensureTargetTypeUnset(option);
                arguments.targetType = AnalyzerTargetType.JDBC;
            } else if ("-cj".equals(option)) {
                String[] values = splitConnectionSpec(requireValue(args, index++, option), option);
                arguments.targetJdbcUrl = values[0];
                arguments.targetUser = values[1];
                arguments.targetPassword = values[2];
            } else {
                throw new IllegalArgumentException(
                        "Unknown option: " + option + System.lineSeparator() + usage());
            }
        }

        arguments.validate();
        return arguments;
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
                + System.lineSeparator()
                + "Options:" + System.lineSeparator()
                + "  -jr <path>   Optional JDBC driver repository directory"
                + System.lineSeparator()
                + "  -so          Source is Oracle JDBC" + System.lineSeparator()
                + "  -oj <spec>   Oracle connection spec: <jdbcUrl|user|password>"
                + System.lineSeparator()
                + "  -sx          Source is XML directory" + System.lineSeparator()
                + "  -xd <path>   XML directory path" + System.lineSeparator()
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

    private void ensureSourceTypeUnset(String option) {
        if (sourceType != null) {
            throw new IllegalArgumentException(
                    "Only one source option is allowed: " + option + System.lineSeparator() + usage());
        }
    }

    private void ensureTargetTypeUnset(String option) {
        if (targetType != null) {
            throw new IllegalArgumentException(
                    "Only one target option is allowed: " + option + System.lineSeparator() + usage());
        }
    }

    private static String requireValue(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException(
                    "Missing value for " + option + System.lineSeparator() + usage());
        }
        return args[index];
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
