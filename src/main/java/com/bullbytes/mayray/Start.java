package com.bullbytes.mayray;

import com.bullbytes.mayray.http.RequestHandlers;
import com.bullbytes.mayray.utils.Lists;
import com.bullbytes.mayray.utils.Ports;
import com.bullbytes.mayray.utils.Ranges;
import com.bullbytes.mayray.utils.log.LoggingConfigurator;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;

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
        LoggingConfigurator.configureLogHandlers(appName);
        logRuntimeInfo();

        var host = "0.0.0.0";

        var portRange = Lists.of(80, Ranges.closed(8080, 9020));
        var portMaybe = portRange.stream()
                .filter(portNr -> Ports.canBind(host, portNr))
                .findFirst();

        portMaybe.ifPresentOrElse(port -> {
            var address = new InetSocketAddress(host, port);
            log.info("Starting server at {}", address);
            try {
                var server = HttpServer.create(address, 0);
                log.info("Server created");
                RequestHandlers.addHandlers(server);
                server.start();
                log.info("Server started");
            } catch (IOException e) {
                log.error("Could not create server at address {}", address, e);
            }

        }, () -> log.error("Could not find open port in this range: {}", portRange));

    }
}
