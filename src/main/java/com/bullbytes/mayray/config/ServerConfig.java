package com.bullbytes.mayray.config;

import com.bullbytes.mayray.utils.PasswordUtil;

import java.nio.file.Path;

/**
 * Contains parameters for our server such as the address and port it listens to.
 * <p>
 * Person of contact: Matthias Braun
 */
public final class ServerConfig {

    private final String host;
    private final int port;
    private final char[] keyStorePassword;
    private final Path keyStorePath;

    ServerConfig(String host, int port, Path keyStorePath, char[] keyStorePassword) {
        this.host = host;
        this.port = port;
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;
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
     * Note that this password can be overwritten using {@link #wipePasswords()}. After that, the contents of passwords
     * are undefined.
     * <p>
     * Callers that store this password as a field, are responsible of overwriting it themselves, as soon as they
     * don't need the password anymore.
     *
     * @return a copy of the keystore password
     */
    public char[] getKeyStorePassword() {
        return keyStorePassword.clone();
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
        PasswordUtil.overwrite(keyStorePassword);
    }
}
