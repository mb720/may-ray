package com.bullbytes.mayray.http.headers;

/**
 * Headers in HTTP requests and responses.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum HttpHeader {
    CONTENT_DISPOSITION("Content-Disposition"),
    ALLOW("Allow"),
    CONTENT_LENGTH("Content-Length"),
    CONTENT_TYPE("Content-Type");

    private final String text;

    HttpHeader(String text) {this.text = text;}

    @Override
    public String toString() {
        return text;
    }
}
