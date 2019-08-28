package com.bullbytes.mayray.http.requests;

import com.bullbytes.mayray.http.headers.HeaderUtil;
import com.bullbytes.mayray.utils.FailMessage;
import com.bullbytes.mayray.utils.ReaderUtil;
import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helps with processing client requests.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum Requests {
    ;

    private static final Logger log = LoggerFactory.getLogger(Requests.class);

    public static Either<FailMessage, String> getBody(Request request) {
        return HeaderUtil.getContentLength(request.getHeaders())
                .flatMap(contentLength ->
                        ReaderUtil.readChars(request.getBody(), contentLength)
                                .map(String::new)
                                .peekLeft(error ->
                                        log.warn("Could not read {} chars (according to content length) from request",
                                                contentLength)));
    }

}
