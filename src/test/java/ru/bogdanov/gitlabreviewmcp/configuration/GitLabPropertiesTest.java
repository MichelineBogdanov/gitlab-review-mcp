package ru.bogdanov.gitlabreviewmcp.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

class GitLabPropertiesTest {

    @Test
    void redactsSecretsFromStringRepresentation() {
        GitLabProperties properties = new GitLabProperties();
        properties.setBaseUrl(URI.create("https://gitlab.example.com"));
        properties.setToken("super-secret-token");
        properties.getSsl().setTrustStorePassword("secret-password");

        assertThat(properties.toString()).doesNotContain("super-secret-token");
        assertThat(properties.getSsl().toString()).doesNotContain("secret-password");
    }

    @Test
    void rejectsPlainHttpByDefault() {
        GitLabProperties properties = new GitLabProperties();
        properties.setBaseUrl(URI.create("http://gitlab.example.com"));
        properties.setToken("token");

        assertThat(properties.isValid()).isFalse();
    }

    @Test
    void rejectsCredentialsInBaseUrl() {
        GitLabProperties properties = new GitLabProperties();
        properties.setBaseUrl(URI.create("https://user:password@gitlab.example.com"));
        properties.setToken("token");

        assertThat(properties.isValid()).isFalse();
    }

    @Test
    void reservesResponseCapacityForBase64FileContentAndJsonMetadata() {
        GitLabProperties properties = new GitLabProperties();
        properties.setBaseUrl(URI.create("https://gitlab.example.com"));
        properties.setToken("token");
        properties.setMaxResponseSize(DataSize.ofMegabytes(2));
        properties.setMaxFileSize(DataSize.ofMegabytes(2));

        assertThat(properties.isValid()).isFalse();

        properties.setMaxFileSize(DataSize.ofMegabytes(1));
        assertThat(properties.isValid()).isTrue();
    }
}
