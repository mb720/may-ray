package com.bullbytes.mayray.tls;

import io.vavr.control.Try;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.security.KeyStore;

/**
 * Helps with Hyper Text Transfer Protocol Secure.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum HttpsUtil {
    ;

    /**
     * Creates an {@link SSLContext} that can be used to encrypt communications between server and client.
     *
     * @param keyStorePath     the server certificates are in a {@link KeyStore} at this {@link Path}
     * @param keyStorePassword the password of the {@link KeyStore}
     * @return an initialized {@link SSLContext} wrapped in a {@link Try} in case something went wrong (e.g., the
     * password for the key store was incorrect)
     */
    public static Try<SSLContext> getSslContext(Path keyStorePath, char[] keyStorePassword) {

        return Try.of(() -> {
            var sslContext = SSLContext.getInstance("TLS");

            var keyStore = KeyStore.getInstance("JKS");

            keyStore.load(new FileInputStream(keyStorePath.toFile()), keyStorePassword);

            String algorithm = "SunX509";

            var keyManagerFactory = KeyManagerFactory.getInstance(algorithm);
            keyManagerFactory.init(keyStore, keyStorePassword);

            var trustManagerFactory = TrustManagerFactory.getInstance(algorithm);
            trustManagerFactory.init(keyStore);

            sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
            return sslContext;
        });
    }
}
