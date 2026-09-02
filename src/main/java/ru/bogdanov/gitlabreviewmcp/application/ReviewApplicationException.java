package ru.bogdanov.gitlabreviewmcp.application;

/**
 * Safe application error that can be returned from an MCP tool.
 */
public class ReviewApplicationException extends RuntimeException {

    private final String code;

    /**
     * Creates a safe application error.
     *
     * @param code stable error code
     * @param message safe message
     */
    public ReviewApplicationException(String code, String message) {
        super(message);
        this.code = code;
    }

    /** @return stable error code */
    public String code() {
        return code;
    }
}
