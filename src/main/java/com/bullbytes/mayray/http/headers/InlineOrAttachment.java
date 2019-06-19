package com.bullbytes.mayray.http.headers;

/**
 * Used as a value of the "Content-Disposition" HTTP header:
 * Defines whether a file should be displayed in the browser (inline) or if the browser should offer to download the
 * file (attachment).
 * <p>
 * Person of contact: Matthias Braun
 *
 * @link https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Disposition
 */
public enum InlineOrAttachment {
    INLINE("inline"), ATTACHMENT("attachment");


    private final String headerString;

    InlineOrAttachment(String headerString) {this.headerString = headerString;}

    @Override
    public String toString() {
        return headerString;
    }
}
