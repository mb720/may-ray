package com.bullbytes.mayray.http.requests;


import com.bullbytes.mayray.utils.FailMessage;
import com.bullbytes.mayray.utils.ParseUtil;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import io.vavr.collection.Traversable;
import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.util.Locale;
import java.util.regex.Pattern;

import static com.bullbytes.mayray.http.requests.RequestMethod.*;
import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;

/**
 * The request line, the headers, and the message body of a client request.
 * <p>
 * Person of contact: Matthias Braun
 */
public final class Request {

    /**
     * We want to match the request method, the requested resource, and the HTTP version of the request line:
     * "GET /resource HTTP 1.1"
     */
    private static final Pattern REQUEST_LINE_REGEX = Pattern.compile("(\\w+) (.+) (.+)");
    private static final Logger log = LoggerFactory.getLogger(Request.class);
    private final RequestMethod method;
    private final String resource;
    private final String httpVersion;
    private final Map<String, String> headers;
    private final BufferedReader body;

    private Request(RequestMethod method,
                    String resource,
                    String httpVersion,
                    Map<String, String> headers,
                    BufferedReader body) {

        this.method = method;
        this.resource = resource;
        this.httpVersion = httpVersion;
        this.headers = headers;
        this.body = body;
    }

    /**
     * Creates a {@link Request} from the {@code headerLines} and the {@code bodyStream}.
     *
     * @param headerLines the lines of the header such as "GET /resource HTTP 1.1", "Host: ...", "User-Agent: ..."
     * @param bodyStream  the body of the message, as a {@link BufferedReader}
     * @return the parsed {@link Request} or a {@link FailMessage}
     */
    public static Either<FailMessage, Request> create(Seq<String> headerLines, BufferedReader bodyStream) {
        return headerLines.headOption()
                .toEither(() -> FailMessage.create("Cannot parse request since there are no lines in the header"))
                .flatMap(requestLine -> ParseUtil.getGroups3(REQUEST_LINE_REGEX, requestLine)
                        .toEither(FailMessage.formatted("Could not parse request method, resource, and " +
                                "HTTP version from request line. Request line is '%s'", requestLine))
                        .flatMap(methodResourceAndVersion -> methodResourceAndVersion.apply((methodStr, resource, httpVersion) ->
                                        parseRequestMethod(methodStr)
                                                .map(method -> new Request(method,
                                                        resource,
                                                        httpVersion,
                                                        // The headers come after the request line
                                                        getHeaderMap(headerLines.tail()),
                                                        bodyStream))
                                )
                        ));
    }

    private static Either<FailMessage, RequestMethod> parseRequestMethod(String methodStr) {

        return switch (methodStr.strip().toUpperCase(Locale.ROOT)) {
            case "GET" -> right(GET);
            case "PUT" -> right(PUT);
            case "POST" -> right(POST);
            case "HEAD" -> right(HEAD);
            case "TRACE" -> right(TRACE);
            case "PATCH" -> right(PATCH);
            case "DELETE" -> right(DELETE);
            case "CONNECT" -> right(CONNECT);
            case "OPTIONS" -> right(OPTIONS);
            default -> left(FailMessage.formatted("Unexpected request method: '%s'", methodStr));
        };
    }

    private static Map<String, String> getHeaderMap(Traversable<String> headerLines) {
        return ParseUtil.getKeyValueMap(headerLines,
                ":",
                String::strip,
                String::strip,
                msg -> log.info(msg.toString()));
    }

    /**
     * @return the {@link RequestMethod} of the {@link Request}
     */
    public RequestMethod getMethod() {
        return method;
    }

    /**
     * @return the requested resource
     */
    public String getResource() {
        return resource;
    }

    /**
     * @return the request's body
     */
    public BufferedReader getBody() {
        return body;
    }

    /**
     * @return the headers of the request
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * @return the version of the request's Hypertext Transfer Protocol
     */
    public String getHttpVersion() {
        return httpVersion;
    }
}
