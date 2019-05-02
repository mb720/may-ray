package com.bullbytes.mayray;

import com.bullbytes.mayray.http.RequestMethod;
import com.bullbytes.mayray.http.Requests;
import com.bullbytes.mayray.http.Responses;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

import static com.bullbytes.mayray.http.StatusCode.NOT_FOUND;
import static com.bullbytes.mayray.http.StatusCode.SUCCESS;
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
            addHandlers(server);
            server.start();
            log.info("Server started");
        } catch (IOException e) {
            log.error("Could not create server at address {}", address, e);
        }
    }

    private static void addHandlers(HttpServer server) {
        server.createContext("/", getRootHandler());
        server.createContext("/spj", getSimonPeytonJonesHandler());
        server.createContext("/ada", getAdaHandler());
    }

    private static HttpHandler getAdaHandler() {
        return exchange -> {
            switch (Requests.getMethod(exchange)) {
                case GET:
                    Responses.send(SUCCESS, format("Get ready for some Lovelace!%n"), exchange);
                    break;
                default:
                    unsupportedMethod(exchange);
            }
        };
    }

    private static HttpHandler getSimonPeytonJonesHandler() {
        return exchange -> {
            if (RequestMethod.GET == Requests.getMethod(exchange)) {
                String resource = getRequestedResource(exchange);
                switch (resource) {
                    case "":
                        Responses.send(SUCCESS, format("You can request an 'img.jpg' or a 'quote' of this person%n"), exchange);
                        break;
                    case "/img.jpg":
                        Responses.sendImage(exchange);
                        break;
                    case "/quote":
                        String msg = format("\"When the limestone of imperative programming is worn away, the granite of functional programming will be observed\"%n");
                        Responses.send(SUCCESS, msg, exchange);
                        break;
                    default:
                        Responses.send(NOT_FOUND, format("Sorry, never heard of this %s thing before%n", resource), exchange);
                }
            } else {
                unsupportedMethod(exchange);
            }
        };
    }

    private static String getRequestedResource(HttpExchange exchange) {
        String handlerPath = exchange.getHttpContext().getPath();
        String uri = exchange.getRequestURI().getPath();

        return uri.substring(handlerPath.length());
    }

    private static HttpHandler getRootHandler() {
        return exchange -> {
            switch (Requests.getMethod(exchange)) {
                case GET:
                    Responses.send(SUCCESS, format("You're at the root now%n"), exchange);
                    break;
                default:
                    unsupportedMethod(exchange);
            }
        };
    }

    private static void unsupportedMethod(HttpExchange exchange) throws IOException {
        Responses.send(NOT_FOUND, format("Can't handle request of type %s, sorry%n",
                exchange.getRequestMethod()),
                exchange);
    }
}
