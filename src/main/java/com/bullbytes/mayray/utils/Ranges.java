package com.bullbytes.mayray.utils;

import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

/**
 * Helps with ranges of numbers.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum Ranges {
    ;

    public static List<Integer> closed(int startInclusive, int endInclusive) {
        return IntStream.rangeClosed(startInclusive, endInclusive)
                .boxed()
                .collect(toList());
    }
}
