package net.replier;

import java.util.Map;

/** Thrown when the API answers with "success": false, or when the HTTP call fails. */
public class ReplierException extends RuntimeException {
    private final String errorCode;
    private final Map<String, Object> response;
    private final int httpStatus;

    public ReplierException(String message, String errorCode, Map<String, Object> response, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.response = response;
        this.httpStatus = httpStatus;
    }

    /** API error code, e.g. "0054", or null. */
    public String getErrorCode() { return errorCode; }

    /** Decoded response body, or null. */
    public Map<String, Object> getResponse() { return response; }

    public int getHttpStatus() { return httpStatus; }
}
