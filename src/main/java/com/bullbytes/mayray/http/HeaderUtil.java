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

    public static List<String> getValuesOf(String key, Headers header) {
        // This might be null if the key doesn't exist in the header
        java.util.List<String> values = header.get(key);
        return values == null ?
                List.empty() :
                List.ofAll(values);
    }
}
