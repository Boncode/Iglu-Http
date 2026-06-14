package org.ijsberg.iglu.rest.model;

public class VerboseHttpResponse {
    private int statusCode;
    private String statusLine;
    private String handle;

    public VerboseHttpResponse(int statusCode, String statusLine) {
        this.statusCode = statusCode;
        this.statusLine = statusLine;
    }

    public VerboseHttpResponse(int statusCode, String statusLine, String handle) {
        this.statusCode = statusCode;
        this.statusLine = statusLine;
        this.handle = handle;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusLine() {
        return statusLine;
    }

    public String getHandle() {
        return handle;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public void setStatusLine(String statusLine) {
        this.statusLine = statusLine;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }
}
