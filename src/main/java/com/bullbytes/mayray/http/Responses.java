package com.bullbytes.mayray.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static com.bullbytes.mayray.http.StatusCode.SUCCESS;

/**
 * Responds to HTTP requests.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum Responses {
    ;
    public static final String CONTENT_TYPE = "Content-Type";
    private static final Logger log = LoggerFactory.getLogger(Responses.class);

    /**
     * Responds with plain text.
     *
     * @param status   the {@link StatusCode} sent to the client
     * @param response the textual response
     * @param exchange the {@link HttpExchange} object used to create the response
     * @throws IOException if sending the response failed
     * @see HttpExchange
     */
    public static void send(StatusCode status,
                            String response,
                            HttpExchange exchange) throws IOException {

//        log.info("Handling request from remote address '{}'", exchange.getRemoteAddress());
//        logHeaders(exchange);

        // Consume the request body to make the underlying TCP connection reusable for following exchanges
        readRequestBody(exchange);

        setContentType(exchange, "text/plain");

        Charset utf8 = StandardCharsets.UTF_8;
        exchange.sendResponseHeaders(status.getCode(), response.getBytes(utf8).length);
        // We have to write and close the response body's output stream to send a response to the client.
        // Closing the output stream automatically closes the input stream as well
        try (var outputStream = exchange.getResponseBody()) {
            outputStream.write(response.getBytes(utf8));
        }
    }

    private static void setContentType(HttpExchange exchange, String contentType) {
        exchange.getResponseHeaders().set(CONTENT_TYPE, contentType);
    }

    private static void readRequestBody(HttpExchange exchange) throws IOException {
        try (var inputStream = exchange.getRequestBody()) {
            inputStream.readAllBytes();
        }
    }

    private static void logHeaders(HttpExchange exchange) {
        Headers reqHeaders = exchange.getRequestHeaders();
        log.info("Request headers:");
        reqHeaders.forEach((key, value) -> log.info("{}: {}", key, value));
    }

    public static void sendImage(HttpExchange exchange) throws IOException {
        log.info("Sending image");

        // Consume the request body to make the underlying TCP connection reusable for following exchanges
        readRequestBody(exchange);

        setContentType(exchange, "image/jpeg");

        try (var outputStream = exchange.getResponseBody();
             var resourceAsStream = Responses.class.getResourceAsStream("/images/spj.jpg");) {

            var imageAsBytes = resourceAsStream.readAllBytes();

            exchange.sendResponseHeaders(SUCCESS.getCode(), imageAsBytes.length);

            outputStream.write(imageAsBytes);
        }

        send(SUCCESS, "Not yet implemented", exchange);
    }
}
