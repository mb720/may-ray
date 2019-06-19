package com.bullbytes.mayray.http;

import com.bullbytes.mayray.http.headers.InlineOrAttachment;
import com.sun.net.httpserver.HttpExchange;
import io.vavr.CheckedConsumer;
import io.vavr.Value;
import io.vavr.collection.List;
import j2html.tags.Renderable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static com.bullbytes.mayray.http.StatusCode.METHOD_NOT_ALLOWED;
import static com.bullbytes.mayray.http.StatusCode.SUCCESS;
import static java.lang.String.format;

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
        withResponseBody(exchange,
                outputStream -> {
                    // Consume the request body to make the underlying TCP connection reusable for following exchanges
                    readRequestBody(exchange);

                    setContentType(exchange, "text/plain");

                    exchange.sendResponseHeaders(status.getCode(), getUtf8Bytes(response).length);

                    outputStream.write(getUtf8Bytes(response));
                }
        );
    }

    private static void withResponseBody(HttpExchange exchange,
                                         CheckedConsumer<? super OutputStream> processOutputStream) {
        // We have to write and close the response body's output stream to send a response to the client.
        // Closing the output stream closes the input stream as well
        try (var outputStream = exchange.getResponseBody()) {
            processOutputStream.accept(outputStream);
        } catch (Throwable e) {
            if (shouldLog(e)) {
                log.warn("Error creating response", e);
            }
        }
    }

    private static byte[] getUtf8Bytes(String response) {
        return response.getBytes(StandardCharsets.UTF_8);
    }

    private static void setContentType(HttpExchange exchange, String contentType) {
        exchange.getResponseHeaders().set(CONTENT_TYPE, contentType);
    }

    private static void readRequestBody(HttpExchange exchange) {
        try (var inputStream = exchange.getRequestBody()) {
            // If the input stream was already closed, there are zero available bytes and reading from the stream
            // would throw an exception
            if (inputStream.available() > 0) {
                inputStream.readAllBytes();
            }
        } catch (IOException ex) {
            log.info("Could not read request body", ex);
        }
    }

    private static boolean shouldLog(Throwable e) {
        String msg = e.getMessage();

        // We don't log the case where the client has abruptly ended the connection
        return !("Broken pipe".equals(msg) || "Connection reset by peer".equals(msg));
    }

    public static void sendError(String errorMsg, Throwable error, HttpExchange exchange) {
        log.warn(errorMsg, error);
        sendPlainText(StatusCode.SERVER_ERROR, errorMsg + "\n", exchange);
    }

    public static void sendError(String errorMsg, HttpExchange exchange) {
        log.warn(errorMsg);
        sendPlainText(StatusCode.SERVER_ERROR, errorMsg + "\n", exchange);
    }


    public static void unsupportedMethod(HttpExchange exchange) {
        unsupportedMethod(exchange, List.empty());
    }

    public static void unsupportedMethod(HttpExchange exchange, Value<RequestMethod> allowedMethods) {
        if (!allowedMethods.isEmpty()) {
            var methodsString = allowedMethods.map(Enum::toString).collect(Collectors.joining(", "));
            exchange.getResponseHeaders().add("Allow", methodsString);
        }
        try {
            exchange.sendResponseHeaders(METHOD_NOT_ALLOWED.getCode(), -1);
            exchange.getResponseBody().close();
        } catch (Exception error) {
            sendError("Could not send 'Method not allowed' response", error, exchange);
        }
    }

    public static void sendHeadResponse(String body, HttpExchange exchange) {
        exchange.getResponseHeaders().add("Content-Length", String.valueOf(getUtf8Bytes(body).length));
        try {
            // -1 Means no response body is being sent
            exchange.sendResponseHeaders(SUCCESS.getCode(), -1);
            // The body of a response to a HEAD method must be empty, but we need to close the input stream
            // to send the response
            exchange.getResponseBody().close();
        } catch (Exception error) {
            sendError("Could not send head response", error, exchange);
        }
    }

    public static void sendFile(URL fileUrl, ContentType type, InlineOrAttachment inlineOrAttachment, HttpExchange exchange) {

        try (var outputStream = exchange.getResponseBody();
             var fileStream = fileUrl.openStream()) {

            // Consume the request body to make the underlying TCP connection reusable for following exchanges
            readRequestBody(exchange);

            setContentType(exchange, type.toString());

            var bytesOfFile = fileStream.readAllBytes();

            var fileName = new File(fileUrl.getPath()).getName();

            // "attachment" makes browsers display the "save as" dialog
            exchange.getResponseHeaders().set("Content-Disposition", format("%s; filename=%s", inlineOrAttachment, fileName));
            exchange.sendResponseHeaders(SUCCESS.getCode(), bytesOfFile.length);

            outputStream.write(bytesOfFile);

        } catch (IOException e) {
            if (shouldLog(e)) {
                sendError("Could not send file at " + fileUrl, e, exchange);
            }
        }
    }

    public static void sendHtml(Renderable html, HttpExchange exchange) {
        withResponseBody(exchange, outputStream -> {
            // Consume the request body to make the underlying TCP connection reusable for following exchanges
            readRequestBody(exchange);
            setContentType(exchange, "text/html");

            var htmlBytes = getUtf8Bytes(html.render());

            exchange.sendResponseHeaders(SUCCESS.getCode(), htmlBytes.length);
            outputStream.write(htmlBytes);
        });
    }
}
