package com.bullbytes.mayray.http;

/**
 * Lists different MIME types.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum ContentType {
    ZIP("application/zip"),
    JPEG("image/jpeg");

    private final String mimeString;

    ContentType(String mimeString) {
        this.mimeString = mimeString;
    }

    @Override
    public String toString() {
        return mimeString;
    }
}
