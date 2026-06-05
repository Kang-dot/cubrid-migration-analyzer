package com.cubrid.sqlanalyzer.core.runner;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;

public class PlcsqlChecker {
    private static final int PLCSQL_SYNTAX_ERROR_CODE = -1;
    private static final String PLCSQL_JAR_PROPERTY = "sqlanalyzer.plcsql.jar";

    private Method checkSyntaxMethod;

    public void checkSQL(String query) throws SQLParserException {
        try {
            loadCheckSyntaxMethod().invoke(null, query);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (isSyntaxError(cause)) {
                throw new SQLParserException(formatSyntaxError(cause), cause);
            }
            throw new SQLParserException("PL/CSQL checker failed.", cause);
        } catch (ReflectiveOperationException | MalformedURLException e) {
            throw new SQLParserException("Failed to initialize PL/CSQL checker.", e);
        }
    }

    private synchronized Method loadCheckSyntaxMethod()
            throws ReflectiveOperationException, MalformedURLException, SQLParserException {
        if (checkSyntaxMethod != null) {
            return checkSyntaxMethod;
        }

        URL[] urls = { findPlcsqlJar().toUri().toURL() };
        URLClassLoader loader = new URLClassLoader(urls, ClassLoader.getPlatformClassLoader());
        Class<?> compilerMain = Class.forName(
                "com.cubrid.plcsql.compiler.PlcsqlCompilerMain",
                true,
                loader);
        checkSyntaxMethod = compilerMain.getMethod("checkSyntax", String.class);
        return checkSyntaxMethod;
    }

    private Path findPlcsqlJar() throws SQLParserException {
        String configuredPath = System.getProperty(PLCSQL_JAR_PROPERTY);
        if (configuredPath != null && !configuredPath.isBlank()) {
            return requireExistingPath(Paths.get(configuredPath));
        }

        Path workingDirectoryPath = Paths.get("pl_server", "pl_server.jar");
        if (Files.isRegularFile(workingDirectoryPath)) {
            return workingDirectoryPath.toAbsolutePath().normalize();
        }

        Path codePath = getCodePath();
        if (codePath != null) {
            Path basePath = Files.isRegularFile(codePath) ? codePath.getParent() : codePath;
            Path packagedPath = basePath.resolve(Paths.get("pl_server", "pl_server.jar"));
            if (Files.isRegularFile(packagedPath)) {
                return packagedPath.toAbsolutePath().normalize();
            }
        }

        throw new SQLParserException(
                "PL/CSQL checker jar was not found. Set -D"
                        + PLCSQL_JAR_PROPERTY
                        + "=/path/to/pl_server.jar.");
    }

    private Path requireExistingPath(Path path) throws SQLParserException {
        if (Files.isRegularFile(path)) {
            return path.toAbsolutePath().normalize();
        }

        throw new SQLParserException("PL/CSQL checker jar was not found: " + path);
    }

    private Path getCodePath() throws SQLParserException {
        try {
            CodeSource codeSource = PlcsqlChecker.class.getProtectionDomain().getCodeSource();
            if (codeSource == null || codeSource.getLocation() == null) {
                return null;
            }
            return Paths.get(codeSource.getLocation().toURI()).toAbsolutePath().normalize();
        } catch (Exception e) {
            throw new SQLParserException("Failed to locate SQL Analyzer runtime path.", e);
        }
    }

    private boolean isSyntaxError(Throwable error) {
        return error != null
                && "com.cubrid.plcsql.compiler.error.SyntaxError".equals(error.getClass().getName());
    }

    private String formatSyntaxError(Throwable error) throws SQLParserException {
        String message = error.getMessage();
        if (message == null) {
            message = "PL/CSQL SyntaxError message is null.";
        } else if (message.isBlank()) {
            message = "PL/CSQL SyntaxError message is blank.";
        }

        return String.format(
                "In line %d, column %d,%n%nERROR(%d): %s",
                getIntField(error, "line"),
                getIntField(error, "column"),
                PLCSQL_SYNTAX_ERROR_CODE,
                message);
    }

    private int getIntField(Throwable error, String fieldName) throws SQLParserException {
        try {
            Field field = error.getClass().getField(fieldName);
            return field.getInt(error);
        } catch (ReflectiveOperationException e) {
            throw new SQLParserException(
                    "Failed to read PL/CSQL SyntaxError " + fieldName + ".", e);
        }
    }
}
