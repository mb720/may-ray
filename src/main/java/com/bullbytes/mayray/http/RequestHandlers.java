package com.bullbytes.mayray.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import static com.bullbytes.mayray.http.StatusCode.NOT_FOUND;
import static com.bullbytes.mayray.http.StatusCode.SUCCESS;
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
    private static final String IMG = "/img.jpg";
    private static final String INFO_ABOUT_PERSON_RESOURCES = "You can request the 'name', an 'img.jpg', or a 'quote' of this person.\n";

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

        return getPersonHandler(name, imgUrl, quote);
    }

    private static HttpHandler getLinusHandler() {
        var name = "Linus Torvald\ns";
        var imgUrl = "http://cdn.facesofopensource.com/wp-content/uploads/2017/03/16181944/linustorvalds.faces22106.web_.jpg";
        var quote = "\"Intelligence is the ability to avoid doing work, yet getting the work done.\"\n";

        return getPersonHandler(name, imgUrl, quote);
    }

    private static HttpHandler getAdaHandler() {
        var name = "Ada Lovelace\n";
        var imgUrl = "https://upload.wikimedia.org/wikipedia/commons/a/a4/Ada_Lovelace_portrait.jpg";
        var quote = "\"The Analytical Engine has no pretensions whatever to originate anything. " +
                "It can do whatever we know how to order it to perform.\"\n";

        return getPersonHandler(name, imgUrl, quote);
    }

    private static HttpHandler getSimonPeytonJonesHandler() {
        var name = "Simon Peyton Jones\n";
        var imgUrl = "https://www.microsoft.com/en-us/research/wp-content/uploads/2016/08/TEDx-Mar14-1.jpg";
        var quote = "\"When the limestone of imperative programming is worn away, the granite of functional programming will be observed.\"\n";
        return getPersonHandler(name, imgUrl, quote);
    }

    private static void getPersonResource(HttpExchange exchange, String name, String imgUrl, String quote) {
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
            switch (Requests.getMethod(exchange)) {
                case GET:
                    Responses.sendPlainText(SUCCESS, "You're at the root now\n", exchange);
                    break;
                default:
                    unsupportedMethod(exchange);
            }
        };
    }

    private static HttpHandler getPersonHandler(String name, String imgUrl, String quote) {
        return exchange -> {
            if (RequestMethod.GET == Requests.getMethod(exchange)) {
                getPersonResource(exchange, name, imgUrl, quote);
            } else {
                unsupportedMethod(exchange);
            }
        };
    }

    private static void unsupportedMethod(HttpExchange exchange) {
        Responses.sendPlainText(NOT_FOUND, format("Can't handle request of type %s, sorry\n",
                exchange.getRequestMethod()),
                exchange);
    }
}
