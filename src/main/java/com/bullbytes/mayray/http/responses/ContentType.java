package com.bullbytes.mayray.http.responses;

/**
 * Lists different MIME types to classify the body of server responses.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum ContentType {
    ZIP("application/zip"),
    JPEG("image/jpeg"),
    TEXT_PLAIN("text/plain"),
    TEXT_HTML("text/html");

    private final String mimeString;

    ContentType(String mimeString) {
        this.mimeString = mimeString;
    }

    @Override
    public String toString() {
        return mimeString;
    }
}
