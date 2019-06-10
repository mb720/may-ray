package com.bullbytes.mayray.http.requesthandlers;

import com.bullbytes.mayray.http.RequestMethod;
import com.bullbytes.mayray.http.Requests;
import com.bullbytes.mayray.http.Responses;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.vavr.CheckedConsumer;

/**
 * Creates {@link HttpHandler}s that catch exceptions when they occur while handling an {@link HttpExchange}.
 * <p>
 * Person of contact: Matthias Braun
 */
enum HttpHandlers {
    ;

    /**
     * Creates an {@link HttpHandler} that sends an {@link Responses#sendError error} if an exception occurs while
     * handling the {@link HttpExchange}.
     *
     * @param handleExchange a {@link CheckedConsumer} of {@link HttpExchange}. Should this cause an {@link Exception}, we
     *                       catch it and send the response that an internal server error occurred
     * @return an {@link HttpHandler} that catches exceptions
     */
    public static HttpHandler checked(CheckedConsumer<? super HttpExchange> handleExchange) {
        return exchange -> {
            try {
                handleExchange.accept(exchange);
            } catch (Throwable error) {
                Responses.sendError("Could not send response", error, exchange);
            }
        };
    }

    /**
     * Creates a {@link #checked} {@link HttpHandler} for a specific request {@code method}. For all other
     * {@link RequestMethod}s the returned {@link HttpHandler} sends {@link Responses#unsupportedMethod} to the client.
     *
     * @param method         we create a {@link HttpHandler} for this {@link RequestHandlers}
     * @param handleExchange defines how the {@link HttpHandler} processes the {@link HttpExchange}
     * @return a {@link #checked} {@link HttpHandler} for the given request {@code method}
     */
    public static HttpHandler forMethod(RequestMethod method,
                                        CheckedConsumer<? super HttpExchange> handleExchange) {
        return checked(exchange -> {
            if (method == Requests.getMethod(exchange)) {
                handleExchange.accept(exchange);
            } else {
                Responses.unsupportedMethod(exchange);
            }
        });
    }
}
