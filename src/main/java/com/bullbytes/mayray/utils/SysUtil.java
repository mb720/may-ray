package com.bullbytes.mayray.utils;

import io.vavr.control.Try;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Interacts with the underlying operating system.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum SysUtil {
    ;

    /**
     * Calls an executable and returns its output. This includes error messages.
     * <p>
     * The command and its arguments have to be separate strings. For example:
     * <pre>
     * String[] commandArray = {"/usr/bin/bash", "-c", "ss -nap | grep 12345"};
     * SysUtil.call(commandArray)
     * </pre>
     * <p>
     * The executable will execute in its own thread.
     *
     * @param command path to the executable (can be a script, a .bat file, etc.)
     *                and its parameters
     * @return output of the executable as a string wrapped in a {@link Try} in case the executable could not be
     * called or its output could not be read
     */
    public static Try<String> call(String... command) {

        var processBuilder = new ProcessBuilder(command);
        // Don't fail silently → This lets us receive error messages
        processBuilder.redirectErrorStream(true);

        // Start the process and read its output
        return Try.of(() -> toString(processBuilder.start().getInputStream()));
    }

    private static String toString(InputStream stream) throws IOException {
        return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
}
