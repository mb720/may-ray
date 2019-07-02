package com.bullbytes.mayray.config;

import io.vavr.control.Option;

import java.nio.file.Path;
import java.util.Arrays;

/**
 * Contains parameters for our server such as the address and port it listens to.
 * <p>
 * Person of contact: Matthias Braun
 */
public final class ServerConfig {

    private final String host;
    private final int port;
    private final Path keyStorePath;
    private Option<char[]> keyStorePassword;

    ServerConfig(String host, int port, Path keyStorePath, char[] keyStorePassword) {
        this.host = host;
        this.port = port;
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = Option.of(keyStorePassword);
    }


    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    /**
     * Gets a copy of the keystore password that contains the server's certificate.
     * <p>
     * Note that this will return {@link Option.None} after the password was overwritten using {@link #wipePasswords()}.
     * <p>
     * Callers that store this password as a field, are responsible of overwriting it themselves, as soon as they
     * don't need the password anymore.
     *
     * @return a copy of the keystore password
     */
    public Option<char[]> getKeyStorePassword() {
        return keyStorePassword.flatMap(p -> Option.of(p.clone()));
    }

    public Path getKeyStorePath() {
        return keyStorePath;
    }

    /**
     * Overwrites all the passwords that this {@link ServerConfig} holds.
     * <p>
     * After calling this method, methods like {@link #getKeyStorePassword()} return incorrect passwords.
     * <p>
     * We overwrite passwords to reduce the time they are in memory and could therefor be retrieved by an attacker
     * who has access to the JVM.
     */
    public void wipePasswords() {
        keyStorePassword.forEach(p -> Arrays.fill(p, '0'));
        keyStorePassword = Option.none();
    }
}
