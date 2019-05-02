package com.bullbytes.mayray;

import com.bullbytes.mayray.http.RequestHandlers;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Starts our server.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum Start {
    ;
    private static final Logger log = LoggerFactory.getLogger(Start.class);

    /**
     * Starts our server, ready to handle requests.
     *
     * @param args arguments are ignored
     */
    public static void main(String... args) {
        int port = 8080;
        var address = new InetSocketAddress(port);
        log.info("Starting server at {}", address);
        try {
            var server = HttpServer.create(address, 0);
            log.info("Server created");
            RequestHandlers.addHandlers(server);
            server.start();
            log.info("Server started");
        } catch (IOException e) {
            log.error("Could not create server at address {}", address, e);
        }
    }
}
