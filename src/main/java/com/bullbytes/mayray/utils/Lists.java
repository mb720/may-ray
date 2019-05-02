package com.bullbytes.mayray.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Helps with {@link List}s.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum Lists {
    ;

    /**
     * Creates a list consisting of the {@code first} element and the {@code rest}.
     *
     * @param first the first element in the created list
     * @param rest  the elements following the {@code first} element
     * @param <T>   the type of the elements
     */
    public static <T> List<T> of(T first, Collection<? extends T> rest) {
        var list = new ArrayList<T>(1 + rest.size());
        list.add(first);
        list.addAll(rest);
        return list;
    }
}
