package com.bullbytes.mayray.utils;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests the {@link ParseUtil}.
 * <p>
 * Person of contact: Matthias Braun
 */
final class ParseUtilTest {

    @Test
    void testGetGroups3WithTwoGroupsRegex() {

        var regex = Pattern.compile("(\\w+) (.+)");

        var testString = "Hello world";

        var result = ParseUtil.getGroups3(regex, testString);
        assertTrue(result.isEmpty(), "Using getGroups3 with a regex with two groups should fail");
    }

    @Test
    void testGetGroups3WithThreeGroupsRegex() {

        var regex = Pattern.compile("(\\w+) (.+) (.+)");

        var testString = "Hello world :-)";

        ParseUtil.getGroups3(regex, testString).fold(
                () -> {
                    fail(format("Should be able to parse three groups using regex %s from string %s", regex, testString));
                    return false;
                }, tuple3 -> tuple3.apply((first, second, third) -> {
                    assertTrue(testString.startsWith(first), "Parsed first string should be start of original string");
                    assertTrue(testString.contains(second), "Parsed second string should be contained in original string");
                    assertTrue(testString.endsWith(third), "Parsed third string should be end of original string");
                    return true;
                }));
    }
}