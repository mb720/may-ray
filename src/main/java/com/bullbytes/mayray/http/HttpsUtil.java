package com.bullbytes.mayray.http;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import io.vavr.control.Try;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.security.KeyStore;

/**
 * Helps with the Hyper Text Transfer Protocol Secure.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum HttpsUtil {
    ;

    /**
     * Gets a {@link HttpsConfigurator} that's used with an {@link HttpsServer}.
     * <p>
     * We read the server certificate from the {@link KeyStore} at the {@code keyStorePath}.
     *
     * @param keyStorePath     the server certificates are in a {@link KeyStore} at this {@link Path}
     * @param keyStorePassword the password of the {@link KeyStore}
     * @return an initialized {@link HttpsConfigurator}
     */
    public static Try<HttpsConfigurator> getHttpsConfigurator(Path keyStorePath, char[] keyStorePassword) {

        return getSslContext(keyStorePath, keyStorePassword)
                // This makes the connection use the default HTTPS parameters
                .map(sslContext -> new HttpsConfigurator(sslContext) {
                    @Override
                    public void configure(HttpsParameters params) {}
                });
    }

    private static Try<SSLContext> getSslContext(Path keyStorePath, char[] keyStorePassword) {

        return Try.of(() -> {
            var sslContext = SSLContext.getInstance("TLS");

            var keyStore = KeyStore.getInstance("JKS");

            keyStore.load(new FileInputStream(keyStorePath.toFile()), keyStorePassword);

            String algorithm = "SunX509";

            var keyManagerFactory = KeyManagerFactory.getInstance(algorithm);
            keyManagerFactory.init(keyStore, keyStorePassword);

            var trustManagerFactory = TrustManagerFactory.getInstance(algorithm);
            trustManagerFactory.init(keyStore);

            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
            return sslContext;
        });
    }
}
