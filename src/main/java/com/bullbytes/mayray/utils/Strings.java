package com.bullbytes.mayray.utils;

import io.vavr.Tuple2;
import io.vavr.control.Either;

import static java.lang.String.format;

/**
 * Helps with searching and manipulating strings.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum Strings {
    ;

    public static Either<String, Tuple2<String, String>> splitAtFirst(String splitter, String original) {

        int splitterStartIndex = original.indexOf(splitter);

        return splitterStartIndex == -1 ?
                Either.left(format("Could not find string to split at '%s' in target string '%s'", splitter, original)) :
                Either.right(
                        new Tuple2<>(original.substring(0, splitterStartIndex),
                                original.substring(splitterStartIndex + splitter.length())));
    }
}
