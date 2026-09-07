package ru.bogdanov.gitlabreviewmcp.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

/**
 * Validated configuration for access to a single self-hosted GitLab instance.
 */
@Validated
@ConfigurationProperties(prefix = "gitlab")
public final class GitLabProperties {

    private static final long REPOSITORY_FILE_RESPONSE_OVERHEAD_BYTES = 64 * 1024;

    @NotNull
    private URI baseUrl;

    @NotBlank
    private String token;

    @NotNull
    private Duration connectTimeout = Duration.ofSeconds(5);

    @NotNull
    private Duration readTimeout = Duration.ofSeconds(30);

    @NotNull
    private DataSize maxResponseSize = DataSize.ofMegabytes(10);

    @NotNull
    private DataSize maxDiffSize = DataSize.ofMegabytes(5);

    @NotNull
    private DataSize maxFileSize = DataSize.ofMegabytes(2);

    private int maxFiles = 500;

    private int maxFileLinesPerRequest = 500;

    private int maxSearchQueryLength = 500;

    private int maxRetries = 2;

    @Valid
    private Ssl ssl = new Ssl();

    /** @return configured GitLab base URL */
    public URI getBaseUrl() {
        return baseUrl;
    }

    /** @param baseUrl configured GitLab base URL */
    public void setBaseUrl(URI baseUrl) {
        this.baseUrl = baseUrl;
    }

    /** @return personal access token */
    public String getToken() {
        return token;
    }

    /** @param token personal access token */
    public void setToken(String token) {
        this.token = token;
    }

    /** @return connection timeout */
    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    /** @param connectTimeout connection timeout */
    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /** @return response timeout */
    public Duration getReadTimeout() {
        return readTimeout;
    }

    /** @param readTimeout response timeout */
    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    /** @return maximum JSON response size */
    public DataSize getMaxResponseSize() {
        return maxResponseSize;
    }

    /** @param maxResponseSize maximum JSON response size */
    public void setMaxResponseSize(DataSize maxResponseSize) {
        this.maxResponseSize = maxResponseSize;
    }

    /** @return maximum aggregate diff text size */
    public DataSize getMaxDiffSize() {
        return maxDiffSize;
    }

    /** @param maxDiffSize maximum aggregate diff text size */
    public void setMaxDiffSize(DataSize maxDiffSize) {
        this.maxDiffSize = maxDiffSize;
    }

    /** @return maximum decoded repository file size */
    public DataSize getMaxFileSize() {
        return maxFileSize;
    }

    /** @param maxFileSize maximum decoded repository file size */
    public void setMaxFileSize(DataSize maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    /** @return maximum changed files collected by an aggregate request */
    public int getMaxFiles() {
        return maxFiles;
    }

    /** @param maxFiles maximum changed files collected by an aggregate request */
    public void setMaxFiles(int maxFiles) {
        this.maxFiles = maxFiles;
    }

    /** @return maximum file lines returned by one tool call */
    public int getMaxFileLinesPerRequest() {
        return maxFileLinesPerRequest;
    }

    /** @param maxFileLinesPerRequest maximum file lines returned by one tool call */
    public void setMaxFileLinesPerRequest(int maxFileLinesPerRequest) {
        this.maxFileLinesPerRequest = maxFileLinesPerRequest;
    }

    /** @return maximum project code search query length */
    public int getMaxSearchQueryLength() {
        return maxSearchQueryLength;
    }

    /** @param maxSearchQueryLength maximum project code search query length */
    public void setMaxSearchQueryLength(int maxSearchQueryLength) {
        this.maxSearchQueryLength = maxSearchQueryLength;
    }

    /** @return maximum retries for idempotent requests */
    public int getMaxRetries() {
        return maxRetries;
    }

    /** @param maxRetries maximum retries for idempotent requests */
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    /** @return TLS trust store configuration */
    public Ssl getSsl() {
        return ssl;
    }

    /** @param ssl TLS trust store configuration */
    public void setSsl(Ssl ssl) {
        this.ssl = ssl;
    }

    /** @return whether URL and numeric limits are safe */
    @AssertTrue(message = "GitLab URL must use HTTPS and limits must be positive and internally consistent")
    public boolean isValid() {
        if (baseUrl == null || baseUrl.getHost() == null || baseUrl.getRawUserInfo() != null
                || baseUrl.getQuery() != null || baseUrl.getFragment() != null) {
            return false;
        }
        boolean validScheme = "https".equalsIgnoreCase(baseUrl.getScheme());
        return validScheme
                && connectTimeout != null && !connectTimeout.isNegative() && !connectTimeout.isZero()
                && readTimeout != null && !readTimeout.isNegative() && !readTimeout.isZero()
                && maxResponseSize != null && maxResponseSize.toBytes() > 0
                && maxDiffSize != null && maxDiffSize.toBytes() > 0
                && maxFileSize != null && maxFileSize.toBytes() > 0
                && repositoryFileFitsResponseLimit()
                && maxFiles > 0 && maxFiles <= 10_000
                && maxFileLinesPerRequest > 0 && maxFileLinesPerRequest <= 5_000
                && maxSearchQueryLength > 0 && maxSearchQueryLength <= 10_000
                && maxRetries >= 0 && maxRetries <= 5;
    }

    private boolean repositoryFileFitsResponseLimit() {
        long responseBytes = maxResponseSize.toBytes();
        if (responseBytes <= REPOSITORY_FILE_RESPONSE_OVERHEAD_BYTES) {
            return false;
        }
        long availableBase64Bytes = responseBytes - REPOSITORY_FILE_RESPONSE_OVERHEAD_BYTES;
        long maximumDecodedBytes = (availableBase64Bytes / 4) * 3;
        return maxFileSize.toBytes() <= maximumDecodedBytes;
    }

    @Override
    public String toString() {
        return "GitLabProperties{baseUrl=" + baseUrl + ", token=<redacted>, connectTimeout="
                + connectTimeout + ", readTimeout=" + readTimeout + '}';
    }

    /**
     * Optional custom trust store settings.
     */
    public static final class Ssl {

        private Path trustStorePath;
        private String trustStorePassword = "";
        private String trustStoreType = "PKCS12";

        /** @return trust store path or {@code null} */
        public Path getTrustStorePath() {
            return trustStorePath;
        }

        /** @param trustStorePath trust store path */
        public void setTrustStorePath(Path trustStorePath) {
            this.trustStorePath = trustStorePath;
        }

        /** @return trust store password */
        public String getTrustStorePassword() {
            return trustStorePassword;
        }

        /** @param trustStorePassword trust store password */
        public void setTrustStorePassword(String trustStorePassword) {
            this.trustStorePassword = trustStorePassword;
        }

        /** @return trust store type */
        public String getTrustStoreType() {
            return trustStoreType;
        }

        /** @param trustStoreType trust store type */
        public void setTrustStoreType(String trustStoreType) {
            this.trustStoreType = trustStoreType;
        }

        /** @return whether a custom trust store is configured */
        public boolean isConfigured() {
            return trustStorePath != null;
        }

        @Override
        public String toString() {
            return "Ssl{trustStorePath=" + trustStorePath + ", trustStorePassword=<redacted>, trustStoreType="
                    + trustStoreType + '}';
        }
    }
}
