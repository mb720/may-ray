package com.bullbytes.mayray.http.requesthandlers;

import com.bullbytes.mayray.http.Requests;
import com.bullbytes.mayray.http.Responses;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.vavr.collection.List;

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
        return HttpHandlers.catching(exchange -> {
            String rootResponse = "You're at the root now\n";
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
}
