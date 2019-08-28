package com.bullbytes.mayray;

import com.bullbytes.mayray.config.CommandLineArgsParser;
import com.bullbytes.mayray.config.ServerConfig;
import com.bullbytes.mayray.config.ServerConfigParser;
import com.bullbytes.mayray.http.Route;
import com.bullbytes.mayray.http.requests.Request;
import com.bullbytes.mayray.http.requests.Requests;
import com.bullbytes.mayray.http.responses.FileResponses;
import com.bullbytes.mayray.http.responses.PersonResponses;
import com.bullbytes.mayray.http.responses.Responses;
import com.bullbytes.mayray.http.responses.StatusCode;
import com.bullbytes.mayray.http.server.WebServer;
import com.bullbytes.mayray.tls.TlsStatus;
import com.bullbytes.mayray.utils.FormattingUtil;
import com.bullbytes.mayray.utils.SysUtil;
import com.bullbytes.mayray.utils.log.LogConfigurator;
import com.bullbytes.mayray.utils.log.LogUtil;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.regex.Pattern;

import static com.bullbytes.mayray.http.requests.RequestMethod.GET;
import static com.bullbytes.mayray.http.requests.RequestMethod.POST;

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
     * @param args processed by the {@link CommandLineArgsParser}
     */
    public static void main(String... args) {
        var appName = "may-ray";
        configureLogging(appName);

        logRuntimeInfo();

        Tuple2<Path, TlsStatus> configPathAndTlsStatus = CommandLineArgsParser.parse(args);

        Path configPath = configPathAndTlsStatus._1;
        TlsStatus tlsStatus = configPathAndTlsStatus._2;

        ServerConfigParser.fromPropertiesFile(configPath).fold(
                messages -> {
                    log.error("Could not parse config file at: {}", configPath);
                    log.error("Please pass a valid configuration file. " +
                            "For example: './gradlew run --args=\"./config/server.properties\"'");
                    messages.forEach(msg -> log.error(msg.toString()));
                    return false;
                },
                serverConfigAndPath -> {
                    var configFilePath = serverConfigAndPath._2.toAbsolutePath().normalize();
                    log.info("Using server configuration at {}", configFilePath);

                    startServer(serverConfigAndPath._1, tlsStatus);
                    return true;
                });
    }

    private static void startServer(ServerConfig config, TlsStatus tlsStatus) {
        WebServer.go(config, tlsStatus,
                route("Root response", "/", Start::getRootResponse),
                route("Coffee response", "/coffee", request ->
                        Responses.plainText("Can't give you coffee, but here's some tea: 🍵", StatusCode.TEAPOT)),
                route("List files", "/list\\?.+", FileResponses::listFiles),
                route("Get directory", "/get\\?.+", FileResponses::zipDir),
                route("Ada responses", "/ada.*", PersonResponses::ada),
                route("Simon Peyton Jones responses", "/spj.*", PersonResponses::simonPeytonJones),
                route("Linus Torvalds responses", "/linus.*", PersonResponses::linus),
                route("Grace Hopper responses", "/grace.*", PersonResponses::graceHopper),
                route("Log system resources", "/stats.*", request -> {
                    SysUtil.logSystemStats();
                    return Responses.plainText("📊 Now logging system stats on the server");
                })
        );
    }

    private static byte[] getRootResponse(Request request) {
        return switch (request.getMethod()) {
            case GET -> Responses.plainText("The server says hi 👋");
            case POST -> Requests.getBody(request)
                    .fold(error -> Responses.plainText(error.toString(), StatusCode.BAD_REQUEST),
                            body -> Responses.plainText("The server thanks you for your post message: " + body)
                    );
            default -> Responses.unsupportedMethod(List.of(GET, POST));
        };
    }

    private static Route route(String routeName,
                               String resourceRegex,
                               Function<Request, byte[]> requestHandler) {

        return Route.create(Pattern.compile(resourceRegex), routeName, requestHandler);
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
