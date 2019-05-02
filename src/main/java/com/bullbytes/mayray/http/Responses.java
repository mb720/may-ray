package com.bullbytes.mayray.http;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Responds to HTTP requests.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum Responses {
    ;

    public static void send(CommonStatusCode status,
                            String response,
                            HttpExchange exchange) throws IOException {

        Charset utf8 = StandardCharsets.UTF_8;
        exchange.sendResponseHeaders(status.getCode(), response.getBytes(utf8).length);
        // We have to write and close the response body to send a response to the client
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(utf8));
        }
    }
}
