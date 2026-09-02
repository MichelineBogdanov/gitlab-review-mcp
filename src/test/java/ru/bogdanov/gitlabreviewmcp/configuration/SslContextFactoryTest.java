package ru.bogdanov.gitlabreviewmcp.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SslContextFactoryTest {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void loadsConfiguredPkcs12TrustStore() throws Exception {
        char[] password = "changeit".toCharArray();
        Path trustStorePath = temporaryDirectory.resolve("corporate-ca.p12");
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(null, password);
        try (OutputStream output = Files.newOutputStream(trustStorePath)) {
            trustStore.store(output, password);
        }
        GitLabProperties.Ssl settings = new GitLabProperties.Ssl();
        settings.setTrustStorePath(trustStorePath);
        settings.setTrustStorePassword(new String(password));

        SSLContext context = new SslContextFactory().create(settings);

        assertThat(context.getProtocol()).isEqualTo("TLS");
    }
}
