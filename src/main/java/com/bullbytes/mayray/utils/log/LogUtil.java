package com.bullbytes.mayray.utils.log;


import com.bullbytes.mayray.utils.Lists;

import java.util.Optional;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import static java.util.Arrays.asList;

/**
 * Helps with the {@link Logger}s of java.util.logging.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum LogUtil {
    ;

    /**
     * Gets the root {@link Logger}.
     *
     * @return the parent of all loggers from which other loggers inherit log levels and handlers
     * @see <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/logging/overview.html#a1.3">Logging overview</a>
     */
    public static Logger getRootLogger() {
        return Logger.getLogger("");
    }

    /**
     * Gets the {@link ConsoleHandler} of the root logger.
     * <p>
     * Configuring this handler will change how the children of the root logger log to console.
     *
     * @return the {@link ConsoleHandler} of the root logger wrapped in a {@link Optional} if the root logger doesn't
     * have a console handler
     */
    static Optional<Handler> getDefaultConsoleHandler() {
        var rootLogger = getRootLogger();
        return Lists.first(asList(rootLogger.getHandlers()));
    }
}
