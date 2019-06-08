package com.bullbytes.mayray;

import com.bullbytes.mayray.http.requesthandlers.RequestHandlers;
import com.bullbytes.mayray.utils.FormattingUtil;
import com.bullbytes.mayray.utils.Ports;
import com.bullbytes.mayray.utils.Ranges;
import com.bullbytes.mayray.utils.log.LogConfigurator;
import com.bullbytes.mayray.utils.log.LogUtil;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.logging.Level;

/**
 * Starts our server.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum Start {
    ;
    private static final Logger log = LoggerFactory.getLogger(Start.class);

    /**
     * Logs information such as the classpath, JVM arguments, and available heap space.
     */
    private static void logRuntimeInfo() {
        log.info("Java classpath: {}", System.getProperty("java.class.path"));
        log.info("JVM arguments and system properties: {}", ManagementFactory.getRuntimeMXBean().getInputArguments());
        log.info("Maximum heap space: {}", FormattingUtil.humanReadableBytes(Runtime.getRuntime().maxMemory()));
        log.info("Process id: {}", ProcessHandle.current().pid());
        log.info("User name: {}", System.getProperty("user.name"));
        log.info("User's current working directory: {}", System.getProperty("user.dir"));

        log.info("VM name: {}", System.getProperty("java.vm.name"));
        log.info("Java runtime version: {}", System.getProperty("java.runtime.version"));
        log.info("Java class format version: {}", System.getProperty("java.class.version"));
    }

    /**
     * Starts our server, ready to handle requests.
     *
     * @param args arguments are ignored
     */
    public static void main(String... args) {
        var appName = "may-ray";
        configureLogging(appName);

        logRuntimeInfo();

        startServer();
    }

    private static void startServer() {
        var host = "0.0.0.0";

        var portRange = Ranges.closed(8080, 9020);
        var portMaybe = portRange.stream()
                .filter(portNr -> Ports.canBind(host, portNr))
                .findFirst();

        portMaybe.ifPresentOrElse(port -> {
            var address = new InetSocketAddress(host, port);
            log.info("Starting server at {}", address);
            startServer(address);

        }, () -> log.error("Could not find port to bind to in this range: {}", portRange));
    }

    private static void startServer(InetSocketAddress address) {
        try {
            var server = HttpServer.create(address, 0);
            log.info("Server created");
            RequestHandlers.addHandlers(server);
            server.start();
            log.info("Server started");
        } catch (IOException e) {
            log.error("Could not create server at address {}", address, e);
        }
    }

    private static void configureLogging(String appName) {
        Level logLevel = Level.INFO;
        // Note that we can set the log level on both the logger and the log handlers
        LogUtil.getRootLogger().setLevel(logLevel);
        LogConfigurator.configureLogHandlers(appName);

        log.info("Log level: {}", logLevel);
        Arrays.stream(LogUtil.getRootLogger().getHandlers())
                .forEach(handler -> log.info("Handler: '{}'. Log level: {}", handler.getClass(), handler.getLevel()));
    }
}
