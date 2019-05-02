package com.bullbytes.mayray;

import com.bullbytes.mayray.http.RequestHandlers;
import com.bullbytes.mayray.utils.Lists;
import com.bullbytes.mayray.utils.Ports;
import com.bullbytes.mayray.utils.Ranges;
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
        var host = "0.0.0.0";

        var portRange = Lists.of(80, Ranges.closed(8080, 9020));
        var portMaybe = portRange.stream()
                .filter(portNr -> Ports.canBind(host, portNr))
                .findFirst();

        portMaybe.ifPresentOrElse(port -> {
            var address = new InetSocketAddress(host, port);
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

        }, () -> {
            log.error("Could not find open port in this range: {}", portRange);
        });

    }
}
