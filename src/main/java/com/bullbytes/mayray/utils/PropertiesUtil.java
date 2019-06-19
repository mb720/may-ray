package com.bullbytes.mayray.utils;

import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Helps with Java Properties files.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum PropertiesUtil {
    ;

    private static final Logger log = LoggerFactory.getLogger(PropertiesUtil.class);

    /**
     * Reads a <a href="https://en.wikipedia.org/wiki/.properties">properties</a> file at the specified {@code path} and
     * returns its key-value pairs as a map.
     *
     * @param path where the properties file is on the file system
     * @return the properties as a map
     */
    public static Map<String, String> readProperties(Path path) {
        var map = new java.util.HashMap<String, String>();

        var prop = new Properties();

        try (var reader = new FileReader(path.toFile(), StandardCharsets.UTF_8)) {
            prop.load(reader);
            prop.forEach((key, value) ->
                    map.put(String.valueOf(key), String.valueOf(value)));
        } catch (IOException e) {
            log.warn("Could not read properties file at {}", path, e);
        }

        return HashMap.ofAll(map);
    }
}

