package com.bullbytes.mayray.fileaccess;

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
 * A {@link MetaFile} is a file containing information about a directory that can be downloaded from the server. For
 * example, the file contains the password needed to download the directory.
 * <p>
 * Person of contact: Matthias Braun
 */
final class MetaFile {
    private static final Logger log = LoggerFactory.getLogger(MetaFile.class);
    private static final String PASSWORD_KEY = "dir_password";
    private final Map<String, String> keysAndValues;

    private MetaFile(Map<String, String> keysAndValues) {this.keysAndValues = keysAndValues;}

    static Try<MetaFile> readFromDir(Path dirPath) {
        Path metaFilePath = Paths.get(dirPath.toString(), ".meta");
        return Try.of(() -> List.ofAll(Files.readAllLines(metaFilePath, StandardCharsets.UTF_8)))
                .map(MetaFile::parseToMap)
                .map(MetaFile::new);
    }

    private static Map<String, String> parseToMap(List<String> lines) {
        List<Either<String, Tuple2<String, String>>> errorMsgsAndTuples =
                lines.map(l -> Strings.splitAtFirst("=", l));

        return errorMsgsAndTuples
                .flatMap(either -> either.fold(msg -> {
                    log.warn(msg);
                    return Option.none();
                }, tuple ->
                        // Remove leading and trailing whitespace
                        Option.of(tuple.map((key, value)
                                -> new Tuple2<>(key.strip(), value.strip())))))
                .toMap(Function.identity());
    }

    boolean passwordIs(String password) {
        return keysAndValues.get(PASSWORD_KEY)
                .map(passwordFromMetaFile -> passwordFromMetaFile.equals(password))
                .getOrElse(false);
    }
}
