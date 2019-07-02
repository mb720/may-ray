package com.bullbytes.mayray.utils.log;


import com.bullbytes.mayray.utils.DirectoryUtil;
import com.bullbytes.mayray.utils.FileUtil;
import com.bullbytes.mayray.utils.ThreadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.logging.*;

/**
 * Configures Java's logging mechanism such as the log message's format and the location of the log files.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum LogConfigurator {
    ;

    private static final Logger log = LoggerFactory.getLogger(LogConfigurator.class);
    // The directory where the file handler creates the log files
    private static final String LOG_DIR = "logs";

    /**
     * Configures the format used by the default {@link ConsoleHandler} and adds a {@link FileHandler} that logs to
     * a local file.
     *
     * @param appName  the name of the app, used in the log file name
     * @param logLevel the handlers will process the messages of this log level or above that the log creates
     */
    public static void configureLogHandlers(String appName, Level logLevel) {
        Formatter formatter = getCustomFormatter();
        LogUtil.getDefaultConsoleHandler().toJavaOptional().ifPresentOrElse(
                consoleHandler -> {
                    consoleHandler.setLevel(logLevel);
                    consoleHandler.setFormatter(formatter);
                },
                () -> System.err.println("Could not get default ConsoleHandler"));

        addFileHandler(appName, formatter, logLevel);
    }

    private static Formatter getCustomFormatter() {
        return new Formatter() {

            @Override
            public String format(LogRecord record) {

                var dateTime = ZonedDateTime.ofInstant(record.getInstant(), ZoneId.systemDefault());

                int threadId = record.getThreadID();
                String threadName = ThreadUtil.getThread(threadId)
                        .map(Thread::getName)
                        .orElseGet(() -> "Thread with ID " + threadId);

                /*
                 * Formats a log message like this:
                 * <p>
                 * INFO    Server started [2019-05-09 18:08:16 +0200] [com.bullbytes.mayray.Start.lambda$main$1] [main]
                 * <p>
                 * See also: https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Formatter.html
                 */
                var FORMAT_STRING = "%2$-7s %6$s [%1$tF %1$tT %1$tz] [%4$s.%5$s] [%3$s]%n%7$s";
                return String.format(
                        FORMAT_STRING,
                        dateTime,
                        record.getLevel().getName(),
                        threadName,
                        record.getSourceClassName(),
                        record.getSourceMethodName(),
                        record.getMessage(),
                        stackTraceToString(record)
                );
            }
        };
    }

    private static String stackTraceToString(LogRecord record) {
        String throwableAsString;
        if (record.getThrown() != null) {
            var stringWriter = new StringWriter();
            var printWriter = new PrintWriter(stringWriter);
            printWriter.println();
            record.getThrown().printStackTrace(printWriter);
            printWriter.close();
            throwableAsString = stringWriter.toString();
        } else {
            throwableAsString = "";
        }
        return throwableAsString;
    }

    private static void addFileHandler(String appName, Formatter formatter, Level logLevel) {
        Path logFile = getLogFileDestination(appName);
        try {
            DirectoryUtil.createParentDirs(logFile);

            boolean appendMessages = true;
            var fileHandler = new FileHandler(logFile.toString(), appendMessages);
            fileHandler.setFormatter(formatter);
            // The file handler will write messages that the log creates if they are of this level or above
            fileHandler.setLevel(logLevel);
            fileHandler.setEncoding(StandardCharsets.UTF_8.displayName(Locale.ROOT));

            LogUtil.getRootLogger().addHandler(fileHandler);
            log.info("Added FileHandler that logs to {}", logFile.toAbsolutePath().normalize());
        } catch (IOException e) {
            log.warn("Could not create FileHandler that logs to {}. Is the location writable?", logFile, e);
        }
    }

    /**
     * Gets the {@link Path} where our log file is.
     * <p>
     * We first attempt to get a subdirectory of the current directory to put our logs in.
     * <p>
     * If that's not possible because the that directory is not writable for us, we get a subdirectory in the
     * temporary directory of the OS.
     *
     * @param appName the name of the application. We use this in the log file name
     * @return the {@link Path} where our log files go
     */
    private static Path getLogFileDestination(String appName) {
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        // The number of the month, from 1 to 12
        int currentMonth = now.getMonth().getValue();
        String fileName = String.format("%s_%s_%s.log", appName, currentYear, currentMonth);

        Path logFileInCurrentDir = Paths.get(".", LOG_DIR, fileName);

        Path logFile;
        if (FileUtil.canCreateOrIsWritable(logFileInCurrentDir.toFile())) {
            logFile = logFileInCurrentDir;
        } else {
            Path logInTempDir = Paths.get(DirectoryUtil.getTempDir(), LOG_DIR, fileName);
            log.warn("Can't log to file '{}', using temporary directory instead: '{}'", logFileInCurrentDir, logInTempDir);
            logFile = logInTempDir;
        }
        return logFile;
    }
}
