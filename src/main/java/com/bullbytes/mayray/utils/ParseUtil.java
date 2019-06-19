package com.bullbytes.mayray.utils;

import io.vavr.control.Either;
import io.vavr.control.Try;

/**
 * Helps with converting strings to other types.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum ParseUtil {
    ;

    public static Either<FailMessage, Integer> parseInt(String str) {
        return Try.of(() -> Integer.parseInt(str))
                .fold(error -> Either.left(FailMessage.formatted("Could not parse string '%s' to an integer", str)),
                        Either::right
                );
    }
}
