package com.cubrid.sqlanalyzer.core.runner;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

public class PlcsqlChecker implements AutoCloseable {
    private static final int PLCSQL_SYNTAX_ERROR_CODE = -1;
    private static final String PLCSQL_JAR_PROPERTY = "sqlanalyzer.plcsql.jar";
    private static final String PLCSQL_COMPILER_MAIN = "com.cubrid.plcsql.compiler.PlcsqlCompilerMain";
    private static final String PLCSQL_SYNTAX_ERROR = "com.cubrid.plcsql.compiler.error.SyntaxError";

    private volatile PlcsqlBridge bridge;
    private volatile boolean closed;

    public void checkSQL(String query) throws SQLParserException {
        try {
            getBridge().checkSyntax(query);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause != null && PLCSQL_SYNTAX_ERROR.equals(cause.getClass().getName())) {
                try {
                    throw new SQLParserException(formatSyntaxError(cause), cause);
                } catch (ReflectiveOperationException formatFailure) {
                    throw new SQLParserException("Failed to read PL/CSQL syntax error.", formatFailure);
                }
            }
            throw new SQLParserException("PL/CSQL checker failed.", cause == null ? e : cause);
        } catch (SQLParserException e) {
            throw e;
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new SQLParserException("PL/CSQL checker failed.", e);
        }
    }

    private PlcsqlBridge getBridge() throws SQLParserException {
        if (closed) {
            throw new SQLParserException("PL/CSQL checker is closed.");
        }

        PlcsqlBridge current = bridge;
        if (current != null) {
            return current;
        }

        synchronized (this) {
            if (closed) {
                throw new SQLParserException("PL/CSQL checker is closed.");
            }
            if (bridge == null) {
                bridge = createBridge();
            }
            return bridge;
        }
    }

    private PlcsqlBridge createBridge() throws SQLParserException {
        Path jarPath = resolvePlcsqlJar();
        ChildFirstClassLoader loader = null;
        try {
            loader =
                    new ChildFirstClassLoader(
                            new URL[] {jarPath.toUri().toURL()}, PlcsqlChecker.class.getClassLoader());
            Class<?> compilerMain = Class.forName(PLCSQL_COMPILER_MAIN, true, loader);
            Method checkSyntax = compilerMain.getMethod("checkSyntax", String.class);
            return new PlcsqlBridge(loader, checkSyntax);
        } catch (ReflectiveOperationException | RuntimeException | java.io.IOException e) {
            closeQuietly(loader);
            throw new SQLParserException("Failed to load PL/CSQL checker from: " + jarPath, e);
        }
    }

    @Override
    public void close() {
        PlcsqlBridge current;
        synchronized (this) {
            closed = true;
            current = bridge;
            bridge = null;
        }
        if (current != null) {
            current.close();
        }
    }

    private Path resolvePlcsqlJar() throws SQLParserException {
        String configuredPath = System.getProperty(PLCSQL_JAR_PROPERTY);
        if (configuredPath != null && !configuredPath.isBlank()) {
            return requireJar(Path.of(configuredPath));
        }

        Path developmentJar = Path.of("pl_server", "pl_server.jar");
        if (Files.isRegularFile(developmentJar)) {
            return developmentJar;
        }

        Path distributionJar = Path.of("lib", "pl_server.jar");
        if (Files.isRegularFile(distributionJar)) {
            return distributionJar;
        }

        throw new SQLParserException(
                "PL/CSQL checker jar is missing. Set -D"
                        + PLCSQL_JAR_PROPERTY
                        + " or place pl_server.jar under pl_server/.");
    }

    private Path requireJar(Path jarPath) throws SQLParserException {
        if (!Files.isRegularFile(jarPath)) {
            throw new SQLParserException("PL/CSQL checker jar is missing: " + jarPath);
        }
        return jarPath;
    }

    private String formatSyntaxError(Throwable error) throws ReflectiveOperationException {
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

    private int getIntField(Object target, String fieldName) throws ReflectiveOperationException {
        Field field = target.getClass().getField(fieldName);
        return field.getInt(target);
    }

    private static final class PlcsqlBridge {
        private final ChildFirstClassLoader loader;
        private final Method checkSyntax;

        private PlcsqlBridge(ChildFirstClassLoader loader, Method checkSyntax) {
            this.loader = loader;
            this.checkSyntax = checkSyntax;
        }

        private void checkSyntax(String query) throws ReflectiveOperationException {
            Thread thread = Thread.currentThread();
            ClassLoader originalClassLoader = thread.getContextClassLoader();
            thread.setContextClassLoader(loader);
            try {
                checkSyntax.invoke(null, query);
            } finally {
                thread.setContextClassLoader(originalClassLoader);
            }
        }

        private void close() {
            closeQuietly(loader);
        }
    }

    private static void closeQuietly(ChildFirstClassLoader loader) {
        if (loader == null) {
            return;
        }
        try {
            loader.close();
        } catch (IOException ignored) {
        }
    }

    private static final class ChildFirstClassLoader extends URLClassLoader {
        private static final String[] CHILD_FIRST_PACKAGES = {
            "com.cubrid.plcsql.",
            "com.cubrid.jsp.",
            "org.antlr.",
            "org.stringtemplate."
        };

        private ChildFirstClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                Class<?> loadedClass = findLoadedClass(name);
                if (loadedClass == null) {
                    loadedClass = loadClassInternal(name);
                }
                if (resolve) {
                    resolveClass(loadedClass);
                }
                return loadedClass;
            }
        }

        private Class<?> loadClassInternal(String name) throws ClassNotFoundException {
            if (isChildFirst(name)) {
                try {
                    return findClass(name);
                } catch (ClassNotFoundException ignored) {
                    return super.loadClass(name, false);
                }
            }
            return super.loadClass(name, false);
        }

        private boolean isChildFirst(String className) {
            for (String packageName : CHILD_FIRST_PACKAGES) {
                if (className.startsWith(packageName)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public URL getResource(String name) {
            URL resource = findResource(name);
            if (resource != null) {
                return resource;
            }
            return super.getResource(name);
        }
    }
}
