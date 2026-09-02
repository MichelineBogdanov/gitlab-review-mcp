package ru.bogdanov.gitlabreviewmcp.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * Builds an SSL context from the configured corporate trust store.
 */
public final class SslContextFactory {

    /**
     * Creates the default or configured SSL context.
     *
     * @param ssl trust store settings
     * @return initialized SSL context
     */
    public SSLContext create(GitLabProperties.Ssl ssl) {
        if (!ssl.isConfigured()) {
            try {
                return SSLContext.getDefault();
            } catch (GeneralSecurityException exception) {
                throw new IllegalStateException("Cannot initialize the default SSL context", exception);
            }
        }

        char[] password = ssl.getTrustStorePassword().toCharArray();
        try (InputStream input = Files.newInputStream(ssl.getTrustStorePath())) {
            KeyStore trustStore = KeyStore.getInstance(ssl.getTrustStoreType());
            trustStore.load(input, password);
            TrustManagerFactory trustManagers = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            trustManagers.init(trustStore);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, trustManagers.getTrustManagers(), null);
            return context;
        } catch (IOException | GeneralSecurityException exception) {
            throw new IllegalStateException("Cannot initialize the configured GitLab trust store", exception);
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }
}
