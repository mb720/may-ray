package com.bullbytes.mayray.utils;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.collection.Map;
import io.vavr.collection.Traversable;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import static io.vavr.API.Some;

/**
 * Helps with converting strings to other types and regular expressions.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum ParseUtil {
    ;

    public static Either<FailMessage, Integer> parseInt(String str) {
        return Try.of(() -> Integer.parseInt(str))
                .fold(error -> Either.left(FailMessage.formatted("Could not parse string '%s' to an integer", str)),
                        Either::right
                );
    }

    public static Map<String, String> getKeyValueMap(Traversable<String> lines,
                                                     String splitter,
                                                     Consumer<FailMessage> onParseError) {
        return getKeyValueMap(lines, splitter, Function.identity(), Function.identity(), onParseError);
    }

    public static <K, V> Map<K, V> getKeyValueMap(Traversable<String> lines,
                                                  String splitter,
                                                  Function<String, K> keyFunc,
                                                  Function<String, V> valFunc,
                                                  Consumer<FailMessage> onParseError) {
        var msgsAndTuples = lines
                .map(line -> Strings.splitAtFirst(splitter, line));

        var keysAndValues = msgsAndTuples
                .flatMap(either -> either.fold(msg -> {
                    onParseError.accept(msg);
                    return Option.none();
                }, tuple ->
                        Some(new Tuple2<>(keyFunc.apply(tuple._1), valFunc.apply(tuple._2)))));

        return keysAndValues.toMap(Function.identity());
    }

    /**
     * Parses the three matched groups of the {@code regex} from the {@code original}.
     *
     * @param regex    a {@link Pattern} that should have three groups in it and match
     *                 the {@code original}
     * @param original the string we'll match with the {@code regex}
     * @return a {@link Tuple3} of the three matched groups or {@link Option#none} if
     * the {@code regex} did not match
     */
    public static Option<Tuple3<String, String, String>> getGroups3(Pattern regex,
                                                                    CharSequence original) {
        var matcher = regex.matcher(original);
        return matcher.matches() ?
                Some(Tuple.of(matcher.group(1), matcher.group(2), matcher.group(3))) :
                Option.none();
    }
}
