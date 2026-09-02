package ru.bogdanov.gitlabreviewmcp.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;

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
}
