package com.bullbytes.mayray.config;

import com.bullbytes.mayray.utils.FailMessage;
import com.bullbytes.mayray.utils.ParseUtil;
import com.bullbytes.mayray.utils.PropertiesUtil;
import io.vavr.API;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import io.vavr.control.Either;
import io.vavr.control.Validation;

import java.nio.file.Path;

import static io.vavr.API.Invalid;
import static io.vavr.API.Tuple;

/**
 * Parses the {@link ServerConfig} from a file.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum ServerConfigParser {
    ;

    private static final String HOST_KEY = "host";
    private static final String PORT_KEY = "port";
    private static final String KEY_STORE_PASSWORD_KEY = "keystorePassword";
    private static final String KEY_STORE_PATH_KEY = "keystorePath";

    private static Either<FailMessage, Path> getConfigFilePath(String[] args) {
        return Array.of(args).headOption().fold(() ->
                        Either.left(FailMessage.create("Please pass a configuration file. " +
                                "For example: './gradlew run --args=\"./config/server.properties\"'")),
                configPath -> Either.right(Path.of(configPath)));
    }

    /**
     * Parses the {@link ServerConfig} from the {@link java.util.Properties} file whose path is expected to be the
     * first element of {@code args}.
     * <p>
     * For example when using Gradle, the file path can be passed as an argument with
     * <pre>
     * ./gradlew run --args="./config/server.properties"
     * </pre>
     *
     * @param args the command line arguments passed to the program's main method
     * @return the {@link ServerConfig} parsed from the properties file together with its file path or the
     * {@link FailMessage}s if that didn't work
     */
    public static Validation<Seq<FailMessage>, Tuple2<ServerConfig, Path>> fromPropertiesFile(String[] args) {
        return getConfigFilePath(args).fold(
                msg -> Invalid(List.of(msg)),
                configPath -> {
                    var confVal = fromMap(PropertiesUtil.readProperties(configPath));
                    return confVal.map(configFile -> Tuple(configFile, configPath));
                }
        );
    }

    static Validation<Seq<FailMessage>, ServerConfig> fromMap(Map<String, String> propMap) {
        return Validation.combine(
                validateHost(propMap),
                validatePort(propMap),
                validateKeyStorePath(propMap),
                validateKeyStorePassword(propMap))
                .ap(ServerConfig::new);
    }

    private static Validation<FailMessage, Path> validateKeyStorePath(Map<String, String> propMap) {
        return getValue(propMap, KEY_STORE_PATH_KEY)
                .map(Path::of);
    }

    private static Validation<FailMessage, char[]> validateKeyStorePassword(Map<String, String> propMap) {
        return getValue(propMap, KEY_STORE_PASSWORD_KEY)
                .map(String::toCharArray);
    }

    private static Validation<FailMessage, String> validateHost(Map<String, String> propMap) {
        return getValue(propMap, HOST_KEY);
    }

    private static Validation<FailMessage, Integer> validatePort(Map<String, String> propMap) {
        return getValue(propMap, PORT_KEY)
                .flatMap(portStr -> Validation.fromEither(ParseUtil.parseInt(portStr)));
    }

    private static Validation<FailMessage, String> getValue(Map<String, String> propMap, String key) {
        return propMap.get(key)
                .fold(() -> Invalid(FailMessage.formatted("Could not find key %s", key)), API::Valid);
    }
}
