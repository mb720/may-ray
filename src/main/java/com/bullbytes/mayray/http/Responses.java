package com.bullbytes.mayray.http;

import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
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
     * @param response the textual response sent to the client
     * @param exchange the {@link HttpExchange} object used to create the response
     */
    public static void sendPlainText(StatusCode status,
                                     String response,
                                     HttpExchange exchange) {

        // We have to write and close the response body's output stream to sendPlainText a response to the client.
        // Closing the output stream automatically closes the input stream as well
        try (var outputStream = exchange.getResponseBody()) {
            // Consume the request body to make the underlying TCP connection reusable for following exchanges
            readRequestBody(exchange);

            setContentType(exchange, "text/plain");

            Charset utf8 = StandardCharsets.UTF_8;
            exchange.sendResponseHeaders(status.getCode(), response.getBytes(utf8).length);

            outputStream.write(response.getBytes(utf8));
        } catch (IOException e) {
            if (!isBrokenPipe(e)) {
                log.warn("Could not send plain text '{}' with status '{}'", response, status, e);
            }
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

    public static void sendImage(String imgUrl, HttpExchange exchange) {
        try (var outputStream = exchange.getResponseBody();
             var imgAsStream = new URL(imgUrl).openStream()) {

            // Consume the request body to make the underlying TCP connection reusable for following exchanges
            readRequestBody(exchange);

            setContentType(exchange, "image/jpeg");

            var imageAsBytes = imgAsStream.readAllBytes();

            exchange.sendResponseHeaders(SUCCESS.getCode(), imageAsBytes.length);

            outputStream.write(imageAsBytes);

        } catch (IOException e) {
            // We don't log the case when the client has abruptly ended the connection
            if (!isBrokenPipe(e)) {
                log.warn("Could not respond with image at {}", imgUrl, e);
                sendPlainText(StatusCode.SERVER_ERROR, "Error while sending image", exchange);
            }
        }
    }

    private static boolean isBrokenPipe(IOException e) {
        return "Broken pipe".equals(e.getMessage());
    }
}
