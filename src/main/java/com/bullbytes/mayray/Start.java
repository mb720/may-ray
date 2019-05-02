package com.bullbytes.mayray;

import com.bullbytes.mayray.http.Requests;
import com.bullbytes.mayray.http.Responses;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

import static com.bullbytes.mayray.http.CommonStatusCode.NOT_FOUND;
import static com.bullbytes.mayray.http.CommonStatusCode.SUCCESS;
import static java.lang.String.format;

/**
 * Starts our server.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum Start {
    ;
    private static final Logger log = LoggerFactory.getLogger(Start.class);

    /**
     * Starts our server and makes it listen to requests.
     *
     * @param args arguments are ignored
     */
    public static void main(String... args) {
        int port = 8080;
        var address = new InetSocketAddress(port);
        log.info("Starting server at {}", address);
        try {
            var server = HttpServer.create(address, 0);
            log.info("Server created");
            server.createContext("/", getRootHandler());
            server.start();
            log.info("Server started");
        } catch (IOException e) {
            log.warn("Could not create server at address {}", address);
        }
    }

    private static HttpHandler getRootHandler() {
        return exchange -> {
            log.info("Handling request");
            logHeaders(exchange);

            switch (Requests.getMethod(exchange)) {
                case GET:
                    Responses.send(SUCCESS, format("Nice to see you, client%n"), exchange);
                    break;
                default:
                    Responses.send(NOT_FOUND, format("Can't handle request of type %s, sorry%n",
                            exchange.getRequestMethod()),
                            exchange);
            }
        };
    }

    private static void logHeaders(HttpExchange exchange) {
        Headers reqHeaders = exchange.getRequestHeaders();
        log.info("Request headers:");
        reqHeaders.forEach((key, value) -> log.info("{}: {}", key, value));
    }
}
