package com.bullbytes.mayray.http.server;

import com.bullbytes.mayray.config.ServerConfig;
import com.bullbytes.mayray.http.Route;
import com.bullbytes.mayray.http.requests.Request;
import com.bullbytes.mayray.http.responses.Responses;
import com.bullbytes.mayray.http.responses.StatusCode;
import com.bullbytes.mayray.tls.HttpsUtil;
import com.bullbytes.mayray.tls.TlsStatus;
import com.bullbytes.mayray.utils.ThreadUtil;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.regex.Pattern;

import static com.bullbytes.mayray.utils.FormattingUtil.humanReadableBytes;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Provides a minimal HTTP server, with and without TLS.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum WebServer {
    ;

    private static final Logger log = LoggerFactory.getLogger(WebServer.class);

    /**
     * Starts the server.
     * <p>
     * Requests are handled by one of the matching {@code routes} in its own thread.
     *
     * @param config    the {@link ServerConfig} defines for example on which port to listen for new connections
     *                  and where the X.509 certificates are
     * @param tlsStatus whether the {@link WebServer} should use Transport Layer Security
     * @param routes    if the {@link Pattern} of one of these {@link Route} matches, it will turn the incoming
     *                  {@link Request} into an array of bytes which we send to the client as the response
     */
    public static void go(ServerConfig config,
                          TlsStatus tlsStatus,
                          Route... routes) {

        var address = new InetSocketAddress(config.getHost(), config.getPort());

        log.info("Starting server at {} with TLS {}", address, tlsStatus);

        try (var serverSocket = tlsStatus == TlsStatus.ON ?
                getTlsServerSocket(config, address) :
                // Create a server socket without TLS
                new ServerSocket(config.getPort(), 0, address.getAddress())) {

            handleRequests(serverSocket, List.of(routes));
        } catch (Exception e) {
            log.warn("Could not start server at {}", address, e);
        }
    }

    private static void handleRequests(ServerSocket serverSocket,
                                       Seq<Route> routes) {

        var threadPool = ThreadUtil.newCachedThreadPool(8);

        // This endless loop is not CPU-intense since method "accept" blocks until a client has made a connection to
        // the socket
        while (true) {
            try {
                // We'll close the socket inside the lambda passed to the thread pool. If we didn't close it,
                // we'd leak file handles
                var socket = serverSocket.accept();
                // Create a response to the request on a separate thread to handle multiple requests simultaneously
                threadPool.submit(() -> {
                    try ( // Read the client's request from the socket
                          var requestStream = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8.name()));
                          // The server writes its response to the socket's output stream
                          var responseStream = new BufferedOutputStream(socket.getOutputStream())
                    ) {
                        byte[] response = getResponse(requestStream, routes);
                        log.info("About to send a response of size {}", humanReadableBytes(response.length));

                        responseStream.write(response);

                        // It's important to flush the response stream before closing it to make sure any
                        // unsent bytes in the buffer are sent via the socket. Otherwise, the client gets an
                        // incomplete response
                        responseStream.flush();

                        socket.close();
                    } catch (Exception e) {
                        log.warn("Exception while creating response", e);
                    }
                });
            } catch (IOException e) {
                log.warn("Exception while waiting for a client connection", e);
            }
        }
    }

    private static ServerSocket getTlsServerSocket(ServerConfig config, InetSocketAddress address) throws IOException {
        char[] keyStorePassword = config.getKeyStorePassword()
                .getOrElseThrow(() -> new IllegalArgumentException("Can't create server without key store password"));

        // Backlog is the maximum number of pending connections on the socket, 0 means an implementation-specific
        // default is used
        int backlog = 0;

        var sslContext = HttpsUtil.getSslContext(config.getKeyStorePath(), keyStorePassword).get();
        // Bind the socket to the given port and address
        return sslContext.getServerSocketFactory()
                .createServerSocket(address.getPort(), backlog, address.getAddress());
    }

    /**
     * Produces a response to the client's request using one of the provided {@code routes}.
     *
     * @param requestStream this {@link BufferedReader} contains the client's request
     * @param routes        when the {@link Pattern} of one of these {@link Route}s matches the requested resource (read
     *                      from the {@code requestStream}), we use the route to produce a response
     * @return the response for the request as an array of bytes
     */
    private static byte[] getResponse(BufferedReader requestStream,
                                      Seq<Route> routes) {

        // Read the header lines from the request stream. The rest of the stream contains the client's request body.
        // We don't try to parse the body into another object like a list of strings here, instead the route
        // can do that based on the request header (using Content-Length, for example)
        return Request.create(getHeaderLines(requestStream), requestStream)
                .fold(msg -> {
                    log.info("Could not read request from socket: {}", msg);
                    return Responses.plainText("Did not understand request", StatusCode.BAD_REQUEST);
                }, request ->
                        // Get the first route that matches the requested resource to create a response
                        routes
                                .find(route -> route.matches(request.getResource()))
                                .peek(route -> log.info("Using route '{}' for resource '{}'", route.getName(), request.getResource()))
                                .map(route -> route.getResponse(request))
                                .getOrElse(() -> {
                                    log.info("No route for requested resource '{}'", request.getResource());
                                    return Responses.plainText("Resource not found", StatusCode.NOT_FOUND);
                                }));
    }

    private static List<String> getHeaderLines(BufferedReader reader) {

        var headerLines = new ArrayList<String>();
        try {
            var line = reader.readLine();
            // The header is concluded when we see an empty line.
            // The line is null if the end of the stream was reached without reading
            // any characters. This can happen if the client tries to connect with
            // HTTPS while the server expects HTTP
            while (line != null && !line.isEmpty()) {
                headerLines.add(line);
                line = reader.readLine();
            }
        } catch (IOException e) {
            log.warn("Could not read all lines from request", e);
        }
        return List.ofAll(headerLines);
    }
}
