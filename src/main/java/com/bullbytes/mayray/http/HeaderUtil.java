package com.bullbytes.mayray.http;

import com.sun.net.httpserver.Headers;
import io.vavr.collection.List;


/**
 * Helps with HTTP {@link Headers}.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum HeaderUtil {
    ;

    /**
     * Converts the keys and values in the {@link Headers} to string.
     *
     * @param headers we convert these {@link Headers} to string
     * @return a string representation of the {@code headers}
     */
    public static String toString(Headers headers) {
        var stringBuilder = new StringBuilder();

        headers.forEach((key, value) ->
                stringBuilder.append(String.format("%s: %s\n", key, value)));

        return stringBuilder.toString();
    }

    public static List<String> getValuesOf(String key, Headers header) {
        // This might be null if the key doesn't exist in the header
        java.util.List<String> values = header.get(key);
        return values == null ?
                List.empty() :
                List.ofAll(values);
    }
}
