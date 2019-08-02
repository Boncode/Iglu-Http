package org.ijsberg.iglu.rest;

/**
 * Created by jeroe on 24/01/2018.
 */
public class RestException extends RuntimeException {

    private int httpStatusCode;

    public RestException(String message, int httpStatusCode) {
        super(message);
        this.httpStatusCode = httpStatusCode;
    }

    public RestException(String message, int httpStatusCode, Throwable cause) {
        super(message, cause);
        this.httpStatusCode = httpStatusCode;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }
}
