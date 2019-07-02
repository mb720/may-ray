package com.bullbytes.mayray.http;

import com.bullbytes.mayray.config.ServerConfig;
import com.bullbytes.mayray.http.requests.Request;
import com.bullbytes.mayray.http.responses.Responses;
import com.bullbytes.mayray.http.responses.StatusCode;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * A minimal HTTPS server.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum HttpsServer {
    ;

    private static final Logger log = LoggerFactory.getLogger(HttpsServer.class);

    /**
     * Starts the server.
     * <p>
     * Requests are handled by one of the matching {@code requestHandlers} in its own thread.
     *
     * @param config          the {@link ServerConfig} defines for example on which port to listen for new connections
     *                        and where the X.509 certificates are
     * @param requestHandlers if the {@link Pattern} of this pair matches, the {@link Function} will turn the incoming
     *                        {@link Request} into an array of bytes which we send to the client as the response
     */
    @SafeVarargs
    public static void go(ServerConfig config,
                          Tuple2<Pattern, Function<Request, byte[]>>... requestHandlers) {

        var address = new InetSocketAddress(config.getHost(), config.getPort());
        config.getKeyStorePassword().fold(() -> {
                    log.warn("Can't create server without key store password");
                    return false;
                },
                keyStorePassword -> {
                    try (var serverSocket = getServerSocket(address, config.getKeyStorePath(), keyStorePassword)) {

                        // We don't need the passwords after creating the server socket
                        config.wipePasswords();

                        log.info("Start multi-threaded server at {}", address);

                        var threadPool = Executors.newCachedThreadPool();

                        // Used to read from the socket's input and output stream
                        var encoding = StandardCharsets.UTF_8;

                        // This endless loop is not CPU-intense since method "accept" blocks until a client has made a connection to
                        // the socket's port
                        while (true) {
                            try {
                                // We'll close the socket inside the lambda passed to the thread pool
                                var socket = serverSocket.accept();
                                // Create a response to the request on a separate thread to handle multiple requests simultaneously
                                threadPool.submit(() -> {

                                    try ( // Use the socket to read the data the client has sent us
                                          var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), encoding.name()));
                                          var outputStream = socket.getOutputStream()
                                    ) {
                                        outputStream.write(getResponse(reader, requestHandlers));

                                        socket.close();
                                    } catch (Exception e) {
                                        log.warn("Exception while processing connection", e);
                                    }
                                });
                            } catch (IOException e) {
                                log.warn("Exception occurred while handling connection", e);
                            }
                        }
                    } catch (IOException e) {
                        log.warn("Could not start server at {}", address, e);
                    }
                    return true;
                }
        );
    }

    @SafeVarargs
    private static byte[] getResponse(BufferedReader requestStream,
                                      Tuple2<Pattern, Function<Request, byte[]>>... requestHandlers) {

        var requestHandlerList = List.of(requestHandlers);

        // Read the header lines from request stream. The rest of the stream contains the client's request body.
        // We don't try to parse the body into another object like a list of strings here, instead the request handler
        // can do that based on the request header (using Content-Length, for example)
        return Request.create(getHeaderLines(requestStream), requestStream)
                .fold(msg -> {
                    log.info("Could not read request from socket: {}", msg);
                    return Responses.plainText("Did not understand request", StatusCode.BAD_REQUEST);
                }, request ->
                        // Get the first handler that matches the requested resource
                        requestHandlerList
                                .find(regexAndFunc -> regexAndFunc._1.asMatchPredicate().test(request.getResource()))
                                // Get the handler
                                .map(regexAndFunc -> regexAndFunc._2)
                                // Let the handler produce a response
                                .map(handler -> handler.apply(request))
                                // We have no handler for the resource
                                .getOrElse(() -> Responses.plainText("Resource not found", StatusCode.NOT_FOUND)));
    }

    private static List<String> getHeaderLines(BufferedReader reader) {

        var headerLines = new ArrayList<String>();
        try {
            var line = reader.readLine();
            // The header is concluded when we see an empty line
            while (!line.isEmpty()) {
                headerLines.add(line);
                line = reader.readLine();
            }
        } catch (IOException e) {
            log.warn("Could not read all lines from request", e);
        }
        return List.ofAll(headerLines);
    }

    private static ServerSocket getServerSocket(InetSocketAddress address,
                                                Path keyStorePath,
                                                char[] keyStorePassword) throws IOException {

        // Backlog is the maximum number of pending connections on the socket, 0 means an implementation-specific
        // default is used
        int backlog = 0;

        SSLContext context = HttpsUtil.getSslContext(keyStorePath, keyStorePassword).get();
        // Bind the socket to the given port and address
        return context.getServerSocketFactory()
                .createServerSocket(address.getPort(), backlog, address.getAddress());
    }
}
