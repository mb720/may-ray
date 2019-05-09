package com.bullbytes.mayray.utils;

import io.atlassian.fugue.Try;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Helps with {@link Try}s.
 * <p>
 * Person of contact: Matthias Braun
 */
public final class TryUtil {
    private TryUtil() {}

    /**
     * Applies {@code ifFailure} to the {@code tryToFold} if it's a failed, otherwise apply {@code ifSuccess}.
     *
     * @param tryToFold the {@link Try} to which we apply {@code ifFailure} or {@code ifSuccess}
     * @param ifFailure the effect we apply to the exception inside the {@code tryToFold} if it's a failed
     * @param ifSuccess the effect we apply to the value inside the {@code tryToFold} if it's a success
     * @param <T>       the type of the success value in the {@code tryToFold}
     */
    public static <T> void foldToVoid(Try<T> tryToFold, Consumer<Exception> ifFailure, Consumer<T> ifSuccess) {
        EitherUtil.foldToVoid(tryToFold.toEither(), ifFailure, ifSuccess);
    }

    /**
     * Applies {@code ifSuccess} to the {@link Try} if it's a success.
     *
     * @param <T>       the type of the success value in the try
     * @param t         the {@link Try} to which we apply {@code ifSuccess}
     * @param ifSuccess the effect we apply to the value inside the try if it's a success
     * @return the original {@link Try}
     */
    public static <T> Try<T> ifSuccess(Try<T> t, Consumer<T> ifSuccess) {
        foldToVoid(t, e -> {}, ifSuccess);
        return t;
    }

    /**
     * Applies {@code ifFailure} to the {@link Try} if it's a failure.
     *
     * @param t         the {@link Try} to which we apply {@code ifFailure}
     * @param ifFailure the effect we apply to the value inside the try if it's a failure
     * @return the original {@link Try}
     */
    public static <T> Try<T> ifFailure(Try<T> t, Consumer<Exception> ifFailure) {
        foldToVoid(t, ifFailure, res -> {});
        return t;
    }

    /**
     * Converts the {@link Try} to a {@link Stream}. If the {@link Try} was a failed, its containing exception is lost.
     *
     * @param t   the {@link Try} we convert to a {@link Stream}
     * @param <T> the type of the {@link Try}'s success value
     * @return the {@link Try} converted to a {@link Stream}
     */
    public static <T> Stream<T> toStream(Try<T> t) {
        return t.fold(
                ex -> Stream.empty(),
                Stream::of);
    }

    /**
     * Converts the {@link Try} to an {@link Optional}. If the {@link Try} was a failed, its contained exception is lost.
     *
     * @param t   the {@link Try} we convert to a {@link Stream}
     * @param <T> the type of the {@link Try}'s success value
     * @return the {@link Try} converted to an {@link Optional}
     */
    public static <T> Optional<T> toOptional(Try<T> t) {
        return t.toEither().toOptional();
    }

    /**
     * Creates a {@link Try} object from an {@code optional}.
     *
     * @param optional we convert this {@link Optional} into a {@link Try}
     * @param ifEmpty  we put this {@link Exception} inside the {@link Try} if the {@code optional} is empty
     * @param <T>      the type of the value inside the {@code optiona} and {@link Try}
     * @return a {@link Try} created from the {@code optional}
     */
    public static <T> Try<T> fromOptional(Optional<T> optional, Exception ifEmpty) {
        return Optionals.fold(optional, () -> Try.failure(ifEmpty), Try::successful);
    }

    /**
     * Creates a {@link Function} that wraps the value in a {@link Try} into a {@link Stream} if it's a success. If
     * the Try is a failure, we pass the contained exception to the {@code ifFailure} {@link Consumer} and make the
     * created {@link Function} return an empty {@link Stream}.
     * <p>
     * This is useful when {@link Stream#flatMap flat mapping} over a stream of {@link Try}s where we want to perform an
     * effect, such as logging, on the failed {@link Try}s.
     *
     * @param ifFailure we pass the {@link Exception} in the {@link Try} to this {@link Consumer}
     * @param <T>       the type of the contained value inside the {@link Try} if it's a success
     * @return a {@link Function} as described above
     */
    public static <T> Function<Try<T>, Stream<T>> getOr(Consumer<Exception> ifFailure) {
        return t -> t.fold(ex -> {
                    ifFailure.accept(ex);
                    return Stream.empty();
                },
                // The Try is a success: just wrap the element in a Stream
                Stream::of);
    }

    /**
     * Gets the value if {@code aTry} is a success or throws the exception inside {@code aTry} otherwise.
     * <p>
     * This is handy for unit tests where it's no issue to throw exceptions since they are handled predictably
     * (i.e., the test fails). For production code, use {@link Try#map}, {@link Try#flatMap}, or {@link Try#fold} to get
     * at the value inside the {@link Try}.
     *
     * @param aTry the {@link Try} whose value we get or whose exception we throw, respectively
     * @param <T>  the type of {@code aTry}
     * @return the value inside {@code aTry} if it's a success
     * @throws Exception otherwise
     */
    public static <T> T getOrThrow(Try<T> aTry) throws Exception {
        if (aTry.isSuccess()) {
            return aTry.toEither().right().get();
        } else {
            throw aTry.toEither().left().get();
        }
    }

    /**
     * Converts a {@link Try} to a {@link CompletableFuture}.
     *
     * @param aTry the {@link Try} we convert to a {@link CompletableFuture}
     * @param <T>  the type of the value inside the {@link Try} if it's a success
     * @return an already completed {@link CompletableFuture} if {@code aTry} is a success, otherwise a failed
     * {@link CompletableFuture}
     */
    public static <T> CompletableFuture<T> toFuture(Try<T> aTry) {
        return aTry.fold(
                CompletableFuture::failedFuture,
                CompletableFuture::completedFuture);
    }
}
