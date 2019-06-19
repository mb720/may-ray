package com.bullbytes.mayray.utils;

/**
 * Helps with passwords.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum PasswordUtil {
    ;

    public static void overwrite(char[] password) {
        for (int i = 0; i < password.length; i++) {
            password[i] = 0;
        }
    }
}
