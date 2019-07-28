package com.bullbytes.mayray.tls;

import java.util.Locale;

/**
 * Whether our server should use Transport Layer Security for sending messages to the client.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum TlsStatus {
    ON, OFF;

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
