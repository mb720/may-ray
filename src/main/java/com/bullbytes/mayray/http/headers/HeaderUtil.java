package com.bullbytes.mayray.http.headers;

import com.bullbytes.mayray.utils.FailMessage;
import com.bullbytes.mayray.utils.ParseUtil;
import com.bullbytes.mayray.utils.Strings;
import io.vavr.collection.Map;
import io.vavr.control.Either;
import io.vavr.control.Option;

/**
 * Helps with HTTP headers.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum HeaderUtil {
    ;

    public static Option<String> getValueOf(HttpHeader header, Map<String, String> headers) {
        var mapWitUpperCaseKeys = headers.mapKeys(Strings::stripAndUpperCase);
        return mapWitUpperCaseKeys.get(Strings.stripAndUpperCase(header.toString()));
    }

    public static Either<FailMessage, Integer> getContentLength(Map<String, String> headers) {
        return getValueOf(HttpHeader.CONTENT_LENGTH, headers)
                .map(ParseUtil::parseInt)
                .getOrElse(() -> Either.left(FailMessage.create("Did not find Content-Length header among headers")));
    }
}
