package com.bullbytes.mayray.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.bullbytes.mayray.http.StatusCode.*;
import static java.lang.String.format;

/**
 * Provides {@link HttpHandler}s to answer HTTP requests.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum RequestHandlers {
    ;
    private static final String QUOTE = "/quote";
    private static final String NAME = "/name";
    private static final String ROLE = "/role";
    private static final String IMG = "/img.jpg";
    private static final String INFO_ABOUT_PERSON_RESOURCES = "You can request the 'name', an 'img.jpg', a 'quote', or the 'role' of this person.\n";

    public static void addHandlers(HttpServer server) {
        server.createContext("/", getRootHandler());
        server.createContext("/spj", getSimonPeytonJonesHandler());
        server.createContext("/ada", getAdaHandler());
        server.createContext("/linus", getLinusHandler());
        server.createContext("/grace", getGraceHopperHandler());
    }

    private static HttpHandler getGraceHopperHandler() {
        var name = "Grace Hopper\n";
        var imgUrl = "http://ww2.kqed.org/mindshift/wp-content/uploads/sites/23/2014/10/grace-hopper_custom-7e094af0ae451cd447568fd03d9c89ba6bf8b352.jpg";
        var quote = "\"A ship in port is safe, but that's not what ships are built for.\"";
        var role = "Computer engineering pioneer\n";

        return getPersonHandler(name, imgUrl, quote, role);
    }

    private static HttpHandler getLinusHandler() {
        var name = "Linus Torvald\ns";
        var imgUrl = "http://cdn.facesofopensource.com/wp-content/uploads/2017/03/16181944/linustorvalds.faces22106.web_.jpg";
        var quote = "\"Intelligence is the ability to avoid doing work, yet getting the work done.\"\n";
        var role = "Inventor of Linux\n";

        return getPersonHandler(name, imgUrl, quote, role);
    }

    private static HttpHandler getAdaHandler() {
        var name = "Ada Lovelace\n";
        var imgUrl = "https://upload.wikimedia.org/wikipedia/commons/a/a4/Ada_Lovelace_portrait.jpg";
        var quote = "\"The Analytical Engine has no pretensions whatever to originate anything. " +
                "It can do whatever we know how to order it to perform.\"\n";
        var role = "First programmer\n";

        return getPersonHandler(name, imgUrl, quote, role);
    }

    private static HttpHandler getSimonPeytonJonesHandler() {
        var name = "Simon Peyton Jones\n";
        var imgUrl = "https://www.microsoft.com/en-us/research/wp-content/uploads/2016/08/TEDx-Mar14-1.jpg";
        var quote = "\"When the limestone of imperative programming is worn away, the granite of functional programming will be observed.\"\n";
        var role = "Inventor of the Haskell programming language\n";

        return getPersonHandler(name, imgUrl, quote, role);
    }

    private static void getPersonResource(HttpExchange exchange, String name, String imgUrl, String quote, String role) {
        String resource = getRequestedResource(exchange);
        switch (resource) {
            case "":
                Responses.sendPlainText(SUCCESS, INFO_ABOUT_PERSON_RESOURCES, exchange);
                break;
            case IMG:
                Responses.sendImage(imgUrl, exchange);
                break;
            case QUOTE:
                Responses.sendPlainText(SUCCESS, quote, exchange);
                break;
            case NAME:
                Responses.sendPlainText(SUCCESS, name, exchange);
                break;
            case ROLE:
                Responses.sendPlainText(SUCCESS, role, exchange);
                break;
            default:
                Responses.sendPlainText(NOT_FOUND, unknownResource(resource), exchange);
        }
    }

    private static String getRequestedResource(HttpExchange exchange) {
        String handlerPath = exchange.getHttpContext().getPath();
        String uri = exchange.getRequestURI().getPath();
        return uri.substring(handlerPath.length());
    }

    private static String unknownResource(String resource) {
        return format("Sorry, never heard of this %s thing before\n", resource);
    }

    private static HttpHandler getRootHandler() {
        return exchange -> {
            String rootResponse = "You're at the root now\n";
            switch (Requests.getMethod(exchange)) {
                case GET:
                    Responses.sendPlainText(SUCCESS, rootResponse, exchange);
                    break;
                case HEAD:
                    exchange.getResponseHeaders().add("Content-Length", String.valueOf(rootResponse.getBytes(StandardCharsets.UTF_8).length));
                    // -1 Means no response body is being sent
                    exchange.sendResponseHeaders(SUCCESS.getCode(), -1);
                    // The body of a response to a HEAD method must be empty, but we need to close the input stream
                    // to send the response
                    exchange.getResponseBody().close();
                    break;
                default:
                    unsupportedMethod(exchange);
            }
        };
    }

    private static HttpHandler getPersonHandler(String name, String imgUrl, String quote, String role) {
        return exchange -> {
            if (RequestMethod.GET == Requests.getMethod(exchange)) {
                getPersonResource(exchange, name, imgUrl, quote, role);
            } else {
                unsupportedMethod(exchange);
            }
        };
    }

    private static void unsupportedMethod(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Allow", "GET");
        exchange.sendResponseHeaders(METHOD_NOT_ALLOWED.getCode(), -1);
        exchange.getResponseBody().close();
    }
}
