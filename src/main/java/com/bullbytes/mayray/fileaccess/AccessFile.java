package com.bullbytes.mayray.fileaccess;

import com.bullbytes.mayray.utils.FailMessage;
import com.bullbytes.mayray.utils.Strings;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

/**
 * An {@link AccessFile} is a file containing information about accessing directories on the server. For
 * example, the file contains the password required for downloading a directory.
 * <p>
 * Person of contact: Matthias Braun
 */
final class AccessFile {
    private static final Logger log = LoggerFactory.getLogger(AccessFile.class);
    private final Map<Path, String> dirsAndPasswords;

    private AccessFile(Map<Path, String> dirsAndPasswords) {this.dirsAndPasswords = dirsAndPasswords;}

    static Try<AccessFile> readFromDir(Path dirPath) {
        Path accessFilePath = Paths.get(dirPath.toString(), "access");

        return Try.of(() -> List.ofAll(Files.readAllLines(accessFilePath, StandardCharsets.UTF_8)))
                .map(AccessFile::parseToMap)
                .map(AccessFile::new);
    }

    private static Map<Path, String> parseToMap(List<String> lines) {
        List<Either<FailMessage, Tuple2<String, String>>> errorMsgsAndTuples =
                lines.map(l -> Strings.splitAtFirst("=", l));

        return errorMsgsAndTuples
                .flatMap(either -> either.fold(msg -> {
                    log.warn(msg.toString());
                    return Option.none();
                }, tuple ->
                        // Remove leading and trailing whitespace
                        Option.of(tuple.map((path, password)
                                // We normalize the path from the file (and later the path provided in #passwordIs) to
                                // treat paths of different representations ("the_dir" and "./the_dir") as the same
                                -> new Tuple2<>(Path.of(path.strip()).normalize(), password.strip())))))
                .toMap(Function.identity());
    }

    /**
     * Checks if a given {@code password} for accessing a {@code desiredDirectory} is correct. It does so by looking
     * up the password for the {@code desiredDirectory} in the {@link AccessFile} and comparing this password with the
     * provided {@code password}.
     * <p>
     * Since we normalize the directories in the {@link AccessFile} as well as the given {@code desiredDirectory}, the
     * representation of a directory in the {@link AccessFile} and the passed {@code desiredDirectory} can differ:
     * {@link AccessFile} sees {@code ./the_dir} and {@code the_dir} as the same directory {@link Path} and
     * {@link #passwordIs} will return true for this directory if the password matches.
     *
     * @param password         check if this string is the correct password for accessing the {@code desiredDirectory}
     * @param desiredDirectory the {@link Path} of the directory whose password we compare with the given {@code password}
     * @return whether the given {@code password} is correct for accessing the {@code desiredDirectory}
     */
    boolean passwordIs(String password, Path desiredDirectory) {
        return dirsAndPasswords.get(desiredDirectory.normalize())
                .map(passwordFromFile -> passwordFromFile.equals(password))
                .getOrElse(false);
    }
}
