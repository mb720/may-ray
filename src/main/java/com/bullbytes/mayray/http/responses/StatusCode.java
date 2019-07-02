package com.bullbytes.mayray.http.responses;

/**
 * HTTP status codes such as 200 and 404.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum StatusCode {
    SUCCESS(200, "Success"),
    BAD_REQUEST(400, "Bad Request"),
    NOT_FOUND(404, "Not Found"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    TEAPOT(418, "I'm a teapot"),
    SERVER_ERROR(500, "Internal Server Error");

    private final int code;
    private final String text;

    StatusCode(int code, String text) {
        this.text = text;
        this.code = code;
    }


    public int getCode() {
        return code;
    }

    public String getText() {
        return text;
    }

    /**
     * @return "200 Success" or "404 Not Found", for example
     */
    @Override
    public String toString() {
        return code + " " + text;
    }
}
