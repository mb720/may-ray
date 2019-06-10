package com.bullbytes.mayray.utils;

import static java.lang.String.format;

/**
 * Represents why a computation has not succeeded.
 * <p>
 * Person of contact: Matthias Braun
 */
public final class FailMessage {
    private final String message;

    private FailMessage(String message) {
        this.message = message;
    }

    static FailMessage formatted(String formatString, Object... formatArgs) {
        return new FailMessage(format(formatString, formatArgs));
    }

    @Override
    public String toString() {
        return message;
    }
}
