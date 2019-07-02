package com.bullbytes.mayray.utils;

import io.vavr.control.Either;
import io.vavr.control.Try;

import java.io.BufferedReader;

/**
 * Additional methods for {@link java.io.Reader} and its subclasses.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum ReaderUtil {
    ;

    /**
     * Reads a number of characters from the {@code reader}.
     *
     * @param reader          we read characters from this {@link BufferedReader}
     * @param nrOfBytesToRead the amount of bytes we read from the {@code reader}
     * @return a character array containing the read bytes or a {@link FailMessage}
     */
    public static Either<FailMessage, char[]> readChars(BufferedReader reader, int nrOfBytesToRead) {
        return Try.of(() -> {
            var chars = new char[nrOfBytesToRead];
            reader.read(chars, 0, nrOfBytesToRead);
            return chars;
        }).toEither()
                .mapLeft(error -> FailMessage
                        .formatted("Could not read %d bytes from reader. Reason: %s",
                                nrOfBytesToRead,
                                error.getMessage()));
    }
}
