package com.bullbytes.mayray.config;

import com.bullbytes.mayray.tls.TlsStatus;
import com.bullbytes.mayray.utils.ParseUtil;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Parses the command line arguments passed to this application.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum CommandLineArgsParser {
    ;

    private static final Path DEFAULT_CONFIG_FILE = Path.of("./config/server.properties");
    private static final Logger log = LoggerFactory.getLogger(CommandLineArgsParser.class);
    private static final String CONFIG_FILE_FLAG = "--config";
    private static final String TLS_FLAG = "--use-tls";

    public static Tuple2<Path, TlsStatus> parse(String[] args) {
        List<String> argsList = List.of(args);
        Map<String, String> map = ParseUtil.getKeyValueMap(argsList,
                "=", msg -> log.error("Could not parse command line arguments: {}", argsList));

        var configFilePath = map.get(CONFIG_FILE_FLAG)
                .map(s -> Path.of(s))
                .getOrElse(DEFAULT_CONFIG_FILE);

        var tlsStatus = map.get(TLS_FLAG)
                .map(s -> "no".equalsIgnoreCase(s) ?
                        TlsStatus.OFF :
                        TlsStatus.ON)
                .getOrElse(TlsStatus.ON);

        return Tuple.of(configFilePath, tlsStatus);
    }
}
