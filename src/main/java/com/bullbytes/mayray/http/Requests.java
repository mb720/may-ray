package com.bullbytes.mayray.http;

import com.sun.net.httpserver.HttpExchange;

import java.util.Locale;

/**
 * Helps with HTTP requests.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum Requests {
    ;

    public static RequestMethod getMethod(HttpExchange exchange) {
        RequestMethod method;
        var reqMethodLoweredString = exchange.getRequestMethod().toLowerCase(Locale.ROOT);

        switch (reqMethodLoweredString) {
            case "get":
                method = RequestMethod.GET;
                break;
            case "post":
                method = RequestMethod.POST;
                break;
            case "head":
                method = RequestMethod.HEAD;
                break;
            default:
                method = RequestMethod.OTHER;
        }
        return method;
    }
}
