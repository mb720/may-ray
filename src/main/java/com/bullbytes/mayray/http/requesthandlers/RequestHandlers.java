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
            long javaProcessId = ProcessHandle.current().pid();

            long openFileDescriptors = unixBean.getOpenFileDescriptorCount();
            long maxFileDescriptors = unixBean.getMaxFileDescriptorCount();

            int availableProcessors = unixBean.getAvailableProcessors();
            double sysLoadAverage = unixBean.getSystemLoadAverage();
            double recentCpuJvmLoad = unixBean.getProcessCpuLoad();
            double recentCpuSysLoad = unixBean.getSystemCpuLoad();
            long processCpuTime = unixBean.getProcessCpuTime();

            String osArchitecture = unixBean.getArch();
            String osVersion = unixBean.getVersion();

            long committedVirtualMemorySize = unixBean.getCommittedVirtualMemorySize();
            long freePhysicalMemorySize = unixBean.getFreePhysicalMemorySize();
            long freeSwapSpaceSize = unixBean.getFreeSwapSpaceSize();

            log.info("OS architecture: '{}', version: {}", osArchitecture, osVersion);
            log.info("Open file descriptors of Java process {}: {} (max: {})", javaProcessId, openFileDescriptors, maxFileDescriptors);

            log.info("Available processors: {}\nLast minute's system load average (runnable entities queued and ran): {}", availableProcessors, sysLoadAverage);
            log.info("Recent JVM CPU load: {}", recentCpuJvmLoad);
            log.info("Recent system CPU load: {}", recentCpuSysLoad);
            log.info("JVM process CPU time in nanoseconds: {}", processCpuTime);

            log.info("Memory guaranteed to be available for JVM process: {}", FormattingUtil.humanReadableBytes(committedVirtualMemorySize));
            log.info("Free physical memory (does not included cached memory): {}", FormattingUtil.humanReadableBytes(freePhysicalMemorySize));
            log.info("Free swap space: {}", FormattingUtil.humanReadableBytes(freeSwapSpaceSize));

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
                    log.info("Current network connections:\n{}", result);
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
