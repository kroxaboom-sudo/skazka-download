package com.kroxaboom.skazka.download;

import java.net.URI;
import java.util.Objects;

public final class HttpTransferResult {
    private final URI finalUri;
    private final int status;
    private final long bytes;
    private final boolean resumed;
    private final String contentType;

    public HttpTransferResult(URI finalUri, int status, long bytes, boolean resumed, String contentType) {
        this.finalUri = finalUri;
        this.status = status;
        this.bytes = bytes;
        this.resumed = resumed;
        this.contentType = contentType;
    }

    public URI finalUri() { return finalUri; }
    public int status() { return status; }
    public long bytes() { return bytes; }
    public boolean resumed() { return resumed; }
    public String contentType() { return contentType; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof HttpTransferResult)) return false;
        HttpTransferResult that = (HttpTransferResult) other;
        return status == that.status && bytes == that.bytes && resumed == that.resumed
                && Objects.equals(finalUri, that.finalUri)
                && Objects.equals(contentType, that.contentType);
    }

    @Override public int hashCode() { return Objects.hash(finalUri, status, bytes, resumed, contentType); }

    @Override public String toString() {
        return "HttpTransferResult[finalUri=" + finalUri + ", status=" + status + ", bytes=" + bytes
                + ", resumed=" + resumed + ", contentType=" + contentType + "]";
    }
}
