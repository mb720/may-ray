package com.bullbytes.mayray.http.requesthandlers;

import com.bullbytes.mayray.http.HeaderUtil;
import com.bullbytes.mayray.http.Requests;
import com.bullbytes.mayray.http.Responses;
import com.bullbytes.mayray.utils.TypeUtil;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsExchange;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSession;
import java.security.cert.Certificate;

import static com.bullbytes.mayray.http.RequestMethod.GET;
import static com.bullbytes.mayray.http.RequestMethod.HEAD;
import static com.bullbytes.mayray.http.StatusCode.SUCCESS;

/**
 * Provides {@link HttpHandler}s to answer HTTP requests.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum RequestHandlers {
    ;

    private static final Logger log = LoggerFactory.getLogger(RequestHandlers.class);

    public static void addHandlers(HttpServer server) {
        server.createContext("/", getRootHandler());
        server.createContext("/spj", PersonRequestHandlers.getSimonPeytonJonesHandler());
        server.createContext("/ada", PersonRequestHandlers.getAdaHandler());
        server.createContext("/linus", PersonRequestHandlers.getLinusHandler());
        server.createContext("/grace", PersonRequestHandlers.getGraceHopperHandler());
        server.createContext("/get", FileRequestHandlers.getDownloadHandler());
        server.createContext("/list", FileRequestHandlers.getListFilesHandler());
    }

    private static HttpHandler getRootHandler() {
        return HttpHandlers.checked(exchange -> {
            String rootResponse = "You're at the root now\n";

            log.info("Request headers:\n{}", HeaderUtil.toString(exchange.getRequestHeaders()));

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
