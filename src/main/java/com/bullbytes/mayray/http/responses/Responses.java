package com.bullbytes.mayray.http.responses;

import com.bullbytes.mayray.http.headers.HttpHeader;
import com.bullbytes.mayray.http.headers.InlineOrAttachment;
import com.bullbytes.mayray.http.requests.RequestMethod;
import io.vavr.collection.Seq;
import j2html.tags.Renderable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.bullbytes.mayray.http.headers.HttpHeader.*;
import static com.bullbytes.mayray.http.responses.ContentType.TEXT_HTML;
import static com.bullbytes.mayray.http.responses.ContentType.TEXT_PLAIN;
import static com.bullbytes.mayray.http.responses.StatusCode.METHOD_NOT_ALLOWED;
import static com.bullbytes.mayray.http.responses.StatusCode.SUCCESS;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

/**
 * Responds to HTTP requests.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum Responses {
    ;

    private static final String HTTP_1_1 = "HTTP/1.1";
    private static final Charset ENCODING = StandardCharsets.UTF_8;
    private static final Logger log = LoggerFactory.getLogger(Responses.class);

    public static byte[] plainText(String body, StatusCode code) {

        var bodyWithNewLine = body + "\r\n";

        String response = statusLine(code) +
                contentLength(bodyWithNewLine) +
                contentType(TEXT_PLAIN) +
                "\r\n" +
                bodyWithNewLine;

        return response.getBytes(ENCODING);
    }

    private static String contentType(ContentType textPlain) {
        return mkHeader(CONTENT_TYPE, format("%s; charset=%s", textPlain, ENCODING.displayName()));
    }

    private static String statusLine(StatusCode code) {
        return HTTP_1_1 + " " + code + "\r\n";
    }

    private static String mkHeader(HttpHeader header, Object value) {
        return header + ": " + value + "\r\n";
    }

    private static String contentLength(String content) {
        return mkHeader(CONTENT_LENGTH, content.getBytes(ENCODING).length);
    }

    public static byte[] plainText(String body) {
        return plainText(body, SUCCESS);
    }

    public static byte[] unsupportedMethod(Seq<RequestMethod> allowedMethods) {

        var allowHeader = allowedMethods.isEmpty() ?
                "" :
                mkHeader(ALLOW,
                        allowedMethods.map(Enum::toString)
                                .collect(joining(", ")));

        return (statusLine(METHOD_NOT_ALLOWED) +
                allowHeader +
                mkHeader(CONTENT_LENGTH, -1)
        ).getBytes(ENCODING);
    }

    public static byte[] html(Renderable htmlToRender) {
        String html = htmlToRender.render();
        return (statusLine(SUCCESS) +
                contentLength(html) +
                contentType(TEXT_HTML) +
                "\r\n" +
                html
        ).getBytes(ENCODING);
    }

    public static byte[] file(URL fileUrl,
                              ContentType contentType,
                              InlineOrAttachment inlineOrAttachment) {

        byte[] response;
        try (var fileStream = fileUrl.openStream()) {
            var bytesOfFile = fileStream.readAllBytes();
            var fileName = new File(fileUrl.getPath()).getName();

            String header = statusLine(SUCCESS) +
                    mkHeader(CONTENT_LENGTH, bytesOfFile.length) +
                    contentType(contentType) +
                    contentDisposition(inlineOrAttachment, fileName)
                    + "\r\n";

            log.info("Header for file response: '{}'", header);

            response = concat(header.getBytes(ENCODING), bytesOfFile);
        } catch (IOException e) {
            var msg = format("Could not read file at URL '%s'", fileUrl);
            log.warn(msg, e);
            response = plainText(msg, StatusCode.SERVER_ERROR);
        }
        return response;
    }

    private static byte[] concat(byte[] first, byte[] second) {
        // New array with contents of first one, having the length of the two input arrays combined
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        // Copy the second array into the result array starting at the end of the first array
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private static String contentDisposition(InlineOrAttachment inlineOrAttachment, String fileName) {
        // "inline" makes the browser try to show the file inside the browser (works for images, for example),
        // "attachment" causes browsers to display the "save as" dialog
        return mkHeader(CONTENT_DISPOSITION,
                format("%s; filename=%s", inlineOrAttachment, fileName));
    }
}
