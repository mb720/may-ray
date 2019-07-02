package com.bullbytes.mayray.utils;

import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Option;

import java.util.Locale;

/**
 * Helps with searching and manipulating strings.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum Strings {
    ;

    public static String getStringAfter(String stringToRemove, String original) {
        // If the original contains the string to remove, get the part after the string to remove. Otherwise, get
        // the original string
        return splitAtFirst(stringToRemove, original)
                .fold(failMessage -> original, Tuple2::_2);
    }

    public static String stripAndUpperCase(String s){
        return s.strip().toUpperCase(Locale.ROOT);
    }


    public static Either<FailMessage, Tuple2<String, String>> splitAtFirst(String splitter, String original) {

        int splitterStartIndex = original.indexOf(splitter);

        return splitterStartIndex == -1 ?
                Either.left(FailMessage.formatted("Could not find string to split at '%s' in target string '%s'", splitter, original)) :
                Either.right(
                        new Tuple2<>(original.substring(0, splitterStartIndex),
                                original.substring(splitterStartIndex + splitter.length())));
    }
}
