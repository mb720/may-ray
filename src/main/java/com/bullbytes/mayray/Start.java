package com.bullbytes.mayray;

import com.bullbytes.mayray.config.ServerConfig;
import com.bullbytes.mayray.config.ServerConfigParser;
import com.bullbytes.mayray.http.HttpsUtil;
import com.bullbytes.mayray.http.requesthandlers.RequestHandlers;
import com.bullbytes.mayray.utils.FormattingUtil;
import com.bullbytes.mayray.utils.log.LogConfigurator;
import com.bullbytes.mayray.utils.log.LogUtil;
import com.sun.net.httpserver.HttpsServer;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.Executors;
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
        log.info("Process ID: {}", ProcessHandle.current().pid());
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

        ServerConfigParser.fromPropertiesFile(args).fold(
                messages -> {
                    messages.forEach(msg -> log.error("Invalid config file: {}", msg));
                    return false;
                },
                (serverConfigAndPath) -> {
                    var configFilePath = serverConfigAndPath._2.toAbsolutePath().normalize();
                    log.info("Read configuration from {}", configFilePath);
                    startServer(serverConfigAndPath._1);
                    return true;
                });
    }

    private static void startServer(ServerConfig config) {
        var address = new InetSocketAddress(config.getHost(), config.getPort());

        log.info("Starting server at {}", address);

        var serverTry = createServer(address, config.getKeyStorePath(), config.getKeyStorePassword());

        serverTry.fold(error -> {
            log.error("Could not create server at address {}", address, error);
            return false;
        }, server -> {
            server.start();
            log.info("Server started. Listening at {}", address);
            return true;
        });
        // We are done using the passwords → Remove them from memory
        config.wipePasswords();
    }

    private static Try<HttpsServer> createServer(InetSocketAddress address,
                                                 Path keyStorePath,
                                                 char[] keyStorePassword) {

        return HttpsUtil.getHttpsConfigurator(keyStorePath, keyStorePassword).flatMap(
                httpsConfigurator -> Try.of(() -> {
                    HttpsServer server = HttpsServer.create(address, 0);
                    server.setExecutor(Executors.newCachedThreadPool());
                    server.setHttpsConfigurator(httpsConfigurator);
                    return RequestHandlers.addHandlers(server);
                }));
    }

    private static void configureLogging(String appName) {
        // Note that we can set the log level on both the logger and the log handlers
        Level logLevel = Level.INFO;
        LogUtil.getRootLogger().setLevel(logLevel);
        LogConfigurator.configureLogHandlers(appName, logLevel);

        log.info("Log level: {}", logLevel);
        Arrays.stream(LogUtil.getRootLogger().getHandlers())
                .forEach(handler -> log.info("Handler: '{}'. Log level: {}", handler.getClass(), handler.getLevel()));
    }
}
