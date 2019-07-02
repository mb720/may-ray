package com.bullbytes.mayray;

import com.bullbytes.mayray.config.ServerConfig;
import com.bullbytes.mayray.config.ServerConfigParser;
import com.bullbytes.mayray.http.HttpsServer;
import com.bullbytes.mayray.http.requests.Request;
import com.bullbytes.mayray.http.requests.Requests;
import com.bullbytes.mayray.http.responses.FileResponses;
import com.bullbytes.mayray.http.responses.PersonResponses;
import com.bullbytes.mayray.http.responses.Responses;
import com.bullbytes.mayray.http.responses.StatusCode;
import com.bullbytes.mayray.utils.FormattingUtil;
import com.bullbytes.mayray.utils.SysUtil;
import com.bullbytes.mayray.utils.log.LogConfigurator;
import com.bullbytes.mayray.utils.log.LogUtil;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
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
    private static final Path DEFAULT_CONFIG_FILE = Path.of("./config/server.properties");

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

        // We interpret the first command line argument as the path to the configuration file. If there are no arguments,
        // we use a default path
        var configPath = Array.of(args).headOption().map(Path::of)
                .getOrElse(DEFAULT_CONFIG_FILE);

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
                    log.info("Read configuration from {}", configFilePath);

                    startNewServer(serverConfigAndPath._1);
                    return true;
                });
    }

    private static void startNewServer(ServerConfig config) {
        HttpsServer.go(config,
                wire("/", Start::getRootResponse),
                wire("/coffee", request ->
                        Responses.plainText("Can't give you coffee, but here's some tea: 🍵", StatusCode.TEAPOT)),
                wire("/list\\?.+", FileResponses::listFiles),
                wire("/get\\?.+", FileResponses::zipDir),
                wire("/ada.*", PersonResponses::ada),
                wire("/spj.*", PersonResponses::simonPeytonJones),
                wire("/linus.*", PersonResponses::linus),
                wire("/grace.*", PersonResponses::graceHopper),
                wire("/stats.*", request -> {
                    SysUtil.logSystemStats();
                    return Responses.plainText("Now logging system stats on the server");
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

    private static Tuple2<Pattern, Function<Request, byte[]>> wire(String resourceRegex,
                                                                   Function<Request, byte[]> handler) {
        return new Tuple2<>(Pattern.compile(resourceRegex), handler);
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
