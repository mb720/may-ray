package com.bullbytes.mayray.utils;

import com.sun.management.UnixOperatingSystemMXBean;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;

import static java.lang.String.format;

/**
 * Interacts with the underlying operating system.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum SysUtil {
    ;

    public static final Logger log = LoggerFactory.getLogger(SysUtil.class);

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

    public static Try<String> callBash(String command) {

        var bash = "/usr/bin/bash";

        var commandOption = "-c";

        return call(bash, commandOption, command);
    }

    private static String toString(InputStream stream) throws IOException {
        return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * Logs information about the system such as the number of free RAM, open file descriptors, currently used sockets,
     * CPU load, etc.
     */
    public static void logSystemStats() {

        TypeUtil.castTo(UnixOperatingSystemMXBean.class, ManagementFactory.getOperatingSystemMXBean()).fold(message -> {
            log.warn(message.toString());
            return false;
        }, unixBean -> {

            logProcessInfo();

            log.info("OS architecture: '{}', version: {}", unixBean.getArch(), unixBean.getVersion());
            log.info("Open file descriptors: {} (max: {})", unixBean.getOpenFileDescriptorCount(), unixBean.getMaxFileDescriptorCount());

            log.info("Available processors: {}", unixBean.getAvailableProcessors());
            log.info("Recent JVM CPU load: {}", unixBean.getProcessCpuLoad());
            log.info("Recent system CPU load: {}", unixBean.getSystemCpuLoad());

            log.info("Memory guaranteed to be available for JVM process: {}", FormattingUtil.humanReadableBytes(unixBean.getCommittedVirtualMemorySize()));
            log.info("Free physical memory (does not include cached memory): {}", FormattingUtil.humanReadableBytes(unixBean.getFreePhysicalMemorySize()));
            log.info("Free swap space: {}", FormattingUtil.humanReadableBytes(unixBean.getFreeSwapSpaceSize()));

            logCurrentSockets();

            logInodeInfo();

            return true;
        });
    }

    private static void logInodeInfo() {

        String command = "df -i";
        callBash(command)
                .fold(error -> {
                    log.warn("Error getting inodes info with command '{}'", command, error);
                    return false;
                }, result -> {
                    log.info("Inodes info:\n{}", result);
                    return true;
                });
    }

    private static void logProcessInfo() {

        ProcessHandle javaProc = ProcessHandle.current();
        log.info("ID of Java process: {}", javaProc.pid());
        javaProc.info().commandLine().ifPresentOrElse(
                cmd -> log.info("Executable path name of Java process and arguments: {}", cmd),
                () -> log.warn("Could not get executable path name and arguments of Java process"));

        javaProc.info().startInstant().ifPresentOrElse(
                startInstant -> log.info("Java process start time: {}", startInstant),
                () -> log.warn("Could not get start time of process"));

    }

    private static void logCurrentSockets() {
        String javaProcessId = String.valueOf(ProcessHandle.current().pid());

        var ssAndGrep = format("ss -nap | grep %s", javaProcessId);

        callBash(ssAndGrep)
                .fold(error -> {
                    log.warn("Error getting sockets of Java process with command '{}'", ssAndGrep, error);
                    return false;
                }, result -> {
                    log.info("Current sockets:\n{}", result);
                    return true;
                });
    }
}
