package com.bullbytes.mayray;

import io.atlassian.fugue.Checked;
import io.atlassian.fugue.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A minimal HTTP server.
 * <p>
 * Person of contact: Matthias Braun
 */
final class NewHttpServer {

    private static final Logger log = LoggerFactory.getLogger(NewHttpServer.class);
    private final ServerSocket serverSocket;

    private NewHttpServer(ServerSocket serverSocket) {

        this.serverSocket = serverSocket;
    }

    static Try<NewHttpServer> create(InetSocketAddress address) {
        // Backlog is the maximum number of pending connections on the socket, 0 means an implementation
        // specific default is used
        int backlog = 0;

        // Bind the socket to the given port and address
        var socketTry = Checked.now(() -> new ServerSocket(address.getPort(), backlog, address.getAddress()));

        return socketTry.map(NewHttpServer::create);
    }

    private static NewHttpServer create(ServerSocket socket) {
        return new NewHttpServer(socket);
    }

    private static List<String> readLinesFromRequest(BufferedReader reader) throws IOException {
        var lines = new ArrayList<String>();
        var line = reader.readLine();
        // An empty line signals the end of the HTTP message
        while (!line.isEmpty()) {
            line = reader.readLine();
            lines.add(line);
        }
        return lines;
    }

    public void awaitConnectionThenShutDown() {
        var charsetName = StandardCharsets.UTF_8.name();
        try (var socket = serverSocket.accept();
             var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), charsetName));
             var writer = new OutputStreamWriter(socket.getOutputStream(), charsetName)
        ) {
            // Use the socket to read the data the client has sent us
            readLinesFromRequest(reader).forEach(log::info);

            var response = "HTTP/1.1 200 OK\r\n\r\n";
            writer.write(response);

            log.info("Response written: {}", response);

        } catch (IOException e) {
            log.warn("Exception occurred while handling connection", e);
        }
    }
}
