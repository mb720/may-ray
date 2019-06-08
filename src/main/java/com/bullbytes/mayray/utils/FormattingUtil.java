package com.bullbytes.mayray.utils;

import java.util.Locale;

/**
 * Formats data such as numbers into strings.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum FormattingUtil {
    ;

    /**
     * Converts a number of bytes into a human-readable string such as `2.2 MB` or `8.0 EiB`. Always uses one
     * digit after the decimal separator.
     *
     * @param bytes  the number of bytes we want to convert
     * @param si     if true, we use base 10 SI units where 1000 bytes are 1 kB.
     *               If false, we use base 2 IEC units where 1024 bytes are 1 KiB.
     * @param locale the {@link Locale} used for formatting the resulting string.
     *               This determines, for example, whether to use comma or dot for the decimal separator
     * @return the bytes as a human-readable string
     */
    public static String humanReadableBytes(long bytes, boolean si, Locale locale) {
        // See https://en.wikipedia.org/wiki/Byte
        int baseValue;
        String[] unitStrings;
        if (si) {
            baseValue = 1000;
            unitStrings = new String[]{"B", "kB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
        } else {
            baseValue = 1024;
            unitStrings = new String[]{"B", "KiB", "MiB", "GiB", "TiB", "PiB", "EiB", "ZiB", "YiB"};
        }

        int exponent = getExponent(bytes, baseValue, 0);
        String unitString = unitStrings[exponent];

        double divisor = StrictMath.pow(baseValue, exponent);
        // Divide the bytes and show one digit after the decimal separator
        return String.format(locale, "%.1f %s", bytes / divisor, unitString);
    }

    /**
     * Converts a number of bytes into a human-readable string
     * such as `60.1 MiB` or `8.0 EiB`. Uses one digit after the decimal separator and
     * base 2 IEC units where 1024 bytes are 1 KiB.
     * <p>
     * The resulting string is formatted using {@link Locale#ROOT} so the decimal separator is a dot.
     *
     * @param bytes the number of bytes we want to convert
     * @return the bytes as a human-readable string
     */
    public static String humanReadableBytes(long bytes) {
        return humanReadableBytes(bytes, false, Locale.ROOT);
    }

    private static int getExponent(long curBytes, int baseValue, int curExponent) {
        if (curBytes < baseValue) {
            return curExponent;
        } else {
            int newExponent = 1 + curExponent;
            return getExponent(curBytes / (baseValue * newExponent), baseValue, newExponent);
        }
    }
}
