package com.bullbytes.mayray.http.requesthandlers;

import com.bullbytes.mayray.http.Requests;
import com.bullbytes.mayray.http.Responses;
import com.bullbytes.mayray.http.headers.HeaderUtil;
import com.bullbytes.mayray.utils.FormattingUtil;
import com.bullbytes.mayray.utils.SysUtil;
import com.bullbytes.mayray.utils.TypeUtil;
import com.sun.management.UnixOperatingSystemMXBean;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsExchange;
import com.sun.net.httpserver.HttpsServer;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSession;
import java.lang.management.ManagementFactory;
import java.security.cert.Certificate;

import static com.bullbytes.mayray.http.RequestMethod.GET;
import static com.bullbytes.mayray.http.RequestMethod.HEAD;
import static com.bullbytes.mayray.http.StatusCode.SUCCESS;
import static java.lang.String.format;

/**
 * Provides {@link HttpHandler}s to answer HTTP requests.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum RequestHandlers {
    ;

    private static final Logger log = LoggerFactory.getLogger(RequestHandlers.class);

    /**
     * Adds {@link HttpHandler} to a {@link HttpServer} so it can process HTTP requests.
     *
     * @param server the {@link HttpServer} to which we add {@link HttpHandler}s
     * @param <T>    the type of server. Used so we can return subtypes of {@link HttpServer}, such as {@link HttpsServer}
     * @return the server with added {@link HttpHandler}s
     */
    public static <T extends HttpServer> T addHandlers(T server) {
        server.createContext("/", getRootHandler());
        server.createContext("/spj", PersonRequestHandlers.getSimonPeytonJonesHandler());
        server.createContext("/ada", PersonRequestHandlers.getAdaHandler());
        server.createContext("/linus", PersonRequestHandlers.getLinusHandler());
        server.createContext("/grace", PersonRequestHandlers.getGraceHopperHandler());
        server.createContext("/get", FileRequestHandlers.getDownloadHandler());
        server.createContext("/list", FileRequestHandlers.getListFilesHandler());
        return server;
    }

    private static HttpHandler getRootHandler() {
        return HttpHandlers.checked(exchange -> {
            String rootResponse = "You're at the root now\n";

            log.info("Request headers:\n{}", HeaderUtil.toString(exchange.getRequestHeaders()));
            logSystemStats();

            TypeUtil.castTo(HttpsExchange.class, exchange)
                    .map(HttpsExchange::getSSLSession)
                    .forEach(RequestHandlers::logSessionInfo);

            switch (Requests.getMethod(exchange)) {
                case GET:
                    Responses.sendPlainText(SUCCESS, rootResponse, exchange);
                    break;
                case HEAD:
                    Responses.sendHeadResponse(rootResponse, exchange);
                    break;
                default:
                    Responses.unsupportedMethod(exchange, List.of(GET, HEAD));
            }
        });
    }

    /**
     * Logs information about the system such as the number of free RAM, open file descriptors, currently used sockets,
     * CPU load, etc.
     */
    private static void logSystemStats() {

        TypeUtil.castTo(UnixOperatingSystemMXBean.class, ManagementFactory.getOperatingSystemMXBean()).fold(message -> {
            log.warn(message.toString());
            return false;
        }, unixBean -> {

            log.info("ID of Java process: {}", ProcessHandle.current().pid());
            log.info("OS architecture: '{}', version: {}", unixBean.getArch(), unixBean.getVersion());
            log.info("Open file descriptors: {} (max: {})", unixBean.getOpenFileDescriptorCount(), unixBean.getMaxFileDescriptorCount());

            log.info("Available processors: {}", unixBean.getAvailableProcessors());
            log.info("Last minute's system load average (runnable entities queued and ran): {}", unixBean.getSystemLoadAverage());
            log.info("Recent JVM CPU load: {}", unixBean.getProcessCpuLoad());
            log.info("Recent system CPU load: {}", unixBean.getSystemCpuLoad());

            log.info("Memory guaranteed to be available for JVM process: {}", FormattingUtil.humanReadableBytes(unixBean.getCommittedVirtualMemorySize()));
            log.info("Free physical memory (does not include cached memory): {}", FormattingUtil.humanReadableBytes(unixBean.getFreePhysicalMemorySize()));
            log.info("Free swap space: {}", FormattingUtil.humanReadableBytes(unixBean.getFreeSwapSpaceSize()));

            logCurrentSockets();

            return true;
        });

    }

    private static void logCurrentSockets() {
        String javaProcessId = String.valueOf(ProcessHandle.current().pid());
        var bash = "/usr/bin/bash";

        var commandOption = "-c";
        var ssAndGrep = format("ss -nap | grep %s", javaProcessId);

        String[] commandArray = {bash, commandOption, ssAndGrep};

        SysUtil.call(commandArray)
                .fold(error -> {
                    log.warn("Error executing command '{}'", String.join(" ", commandArray), error);
                    return false;
                }, result -> {
                    log.info("Current sockets:\n{}", result);
                    return true;
                });
    }

    private static void logSessionInfo(SSLSession sslSession) {
        log.info("Peer host and port: {}:{}", sslSession.getPeerHost(), sslSession.getPeerPort());
        log.info("Cipher suite: {}", sslSession.getCipherSuite());
        log.info("Protocol: {}", sslSession.getProtocol());

        log.info("Certificates sent to peer:");
        Certificate[] localCertificates = sslSession.getLocalCertificates();
        if (localCertificates != null) {
            Stream.of(localCertificates).forEach(certificate -> log.info("Type: {}", certificate.getType()));
        } else {
            log.info("No certificates were sent to the peer");
        }
    }
}
