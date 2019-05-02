package com.bullbytes.mayray.http;

/**
 * Common HTTP status codes such as 200 and 404.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum CommonStatusCode {
    SUCCESS(200), NOT_FOUND(404);

    private final int code;

    CommonStatusCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
