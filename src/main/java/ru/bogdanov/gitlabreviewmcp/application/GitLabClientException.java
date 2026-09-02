package ru.bogdanov.gitlabreviewmcp.application;

/**
 * Safe GitLab adapter failure including retry and ambiguity metadata.
 */
public final class GitLabClientException extends ReviewApplicationException {

    private final Integer httpStatus;
    private final String gitLabRequestId;
    private final boolean ambiguousWrite;

    /**
     * Creates a GitLab client error.
     *
     * @param code stable error code
     * @param message safe message
     * @param httpStatus HTTP status, if available
     * @param gitLabRequestId GitLab request identifier
     * @param ambiguousWrite whether a write might have succeeded
     */
    public GitLabClientException(
            String code,
            String message,
            Integer httpStatus,
            String gitLabRequestId,
            boolean ambiguousWrite) {
        super(code, message);
        this.httpStatus = httpStatus;
        this.gitLabRequestId = gitLabRequestId;
        this.ambiguousWrite = ambiguousWrite;
    }

    /** @return HTTP status, if available */
    public Integer httpStatus() {
        return httpStatus;
    }

    /** @return GitLab request identifier */
    public String gitLabRequestId() {
        return gitLabRequestId;
    }

    /** @return whether a write might have succeeded */
    public boolean ambiguousWrite() {
        return ambiguousWrite;
    }
}
