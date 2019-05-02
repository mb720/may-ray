package com.bullbytes.mayray.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * Helps the ports for establishing TCP or UDP connections.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum Ports {
    ;

    public static boolean canBind(String host, int portNr) {
        boolean portFree;
        try (var ignored = new ServerSocket(portNr, 0, InetAddress.getByName(host))) {
            portFree = true;
        } catch (IOException e) {
            portFree = false;
        }
        return portFree;
    }
}
