package com.bullbytes.mayray.utils;

import static java.lang.String.format;

/**
 * Describes why a computation has not succeeded.
 * <p>
 * Person of contact: Matthias Braun
 */
public final class FailMessage {
    private final String message;

    private FailMessage(String message) {
        this.message = message;
    }

    public static FailMessage create(String message) {
        return new FailMessage(message);
    }

    public static FailMessage formatted(String formatString, Object... formatArgs) {
        return new FailMessage(format(formatString, formatArgs));
    }

    @Override
    public String toString() {
        return message;
    }
}
