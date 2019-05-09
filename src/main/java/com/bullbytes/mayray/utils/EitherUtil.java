package com.bullbytes.mayray.utils;

import io.atlassian.fugue.Either;
import io.atlassian.fugue.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * Helps with {@link Either}s.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum EitherUtil {
    ;
    private static final Logger log = LoggerFactory.getLogger(EitherUtil.class);

    /**
     * Gets all the right values from a collection of {@link Either}s.
     *
     * @param eithers a {@link Collection} of {@link Either}s.
     * @param <L>     the left type of the {@link Either} in {@code eithers}
     * @param <R>     the right type of the {@link Either} in {@code eithers}
     * @return a {@link List} of the right values in the collection of {@link Either}s
     */
    public static <L, R> List<R> getRights(final Collection<Either<L, R>> eithers) {
        return eithers.stream()
                .flatMap(EitherUtil::getRightOrEmptyStream)
                .collect(toList());
    }

    /**
     * Gets all the left values from a collection of {@link Either}s.
     *
     * @param eithers a {@link Collection} of {@link Either}s.
     * @param <L>     the left type of the {@link Either} in {@code eithers}
     * @param <R>     the right type of the {@link Either} in {@code eithers}
     * @return a {@link List} of the left values in the collection of {@link Either}s
     */
    public static <L, R> List<L> getLefts(final Collection<Either<L, R>> eithers) {
        return eithers.stream()
                .flatMap(EitherUtil::getLeftOrEmptyStream)
                .collect(toList());
    }

    /**
     * Converts the {@code either} into an empty {@link Stream} if it is a left or a {@link Stream} containing the single
     * right value of the {@code either} otherwise.
     * <p>
     * This is useful when getting the right values from a collection of {@link Either}s:
     *
     * <pre>
     * {@code List<R>} rightValues = eithers.stream()
     * .flatMap(EitherUtil::getRightOrEmptyStream)
     * .collect(Collectors.toList());
     *
     * </pre>
     *
     * @param either the {@link Either} whose right value we want to turn into a {@link Stream}
     * @param <L>    the type of the {@code either}'s left value
     * @param <R>    the type of the {@code either}'s right value
     * @return an empty {@link Stream} if the {@code either} is a left or a {@link Stream} containing the single
     * right value of the {@code either} otherwise.
     */
    public static <L, R> Stream<R> getRightOrEmptyStream(final Either<L, R> either) {
        return either.isLeft() ? Stream.empty() : Stream.of(either.getOrThrow(() -> noRightValueException(either)));
    }

    /**
     * Executes code when the {@code either} contains a left value.
     *
     * @param either we'll execute the code defined in {@code ifLeft} if this {@link Either} contains a left value
     * @param ifLeft we'll call this {@link Consumer} if the {@code either} contains a left value
     * @param <L>    the type of the {@code either}'s left value
     * @param <R>    the type of the {@code either}'s right value
     */
    public static <L, R> void ifLeft(final Either<L, R> either, final Consumer<L> ifLeft) {
        if (either.isLeft()) {
            final L leftValue = either.left().getOrThrow(() -> noLeftValueException(either));
            ifLeft.accept(leftValue);
        }
    }

    /**
     * Applies {@code ifLeft} to the {@code either} if it's left, otherwise apply {@code ifRight}.
     *
     * @param either  the {@link Either} to which we apply {@code ifLeft} or {@code ifRight}
     * @param ifLeft  the effect we apply to the left value of the {@code either} if it's left
     * @param ifRight the effect we apply to the right value of the {@code either} if it's right
     * @param <L>     the type of the {@code either}'s left value
     * @param <R>     the type of the {@code either}'s right value
     */
    public static <L, R> void foldToVoid(final Either<L, R> either, final Consumer<L> ifLeft, final Consumer<R> ifRight) {
        if (either.isLeft()) {
            final L leftValue = either.left().getOrThrow(() -> noLeftValueException(either));
            ifLeft.accept(leftValue);
        } else if (either.isRight()) {
            final R rightValue = either.right().getOrThrow(() -> noRightValueException(either));
            ifRight.accept(rightValue);
        } else {
            log.warn("Either is neither left nor right: {}", either);
        }
    }

    /**
     * Converts the {@code either} into an empty {@link Stream} if it is a right or a {@link Stream} containing the single
     * left value of the {@code either} otherwise.
     * <p>
     * This is useful when getting the left values from a collection of {@link Either}s:
     *
     * <pre>
     * {@code List<L>} leftValues = eithers.stream()
     * .flatMap(EitherUtil::getLeftOrEmptyStream)
     * .collect(Collectors.toList());
     *
     * </pre>
     *
     * @param either the {@link Either} whose left value we want to turn into a {@link Stream}
     * @param <L>    the type of the {@code either}'s left value
     * @param <R>    the type of the {@code either}'s right value
     * @return an empty {@link Stream} if the {@code either} is a right or a {@link Stream} containing the single
     * left value of the {@code either} otherwise.
     */
    private static <L, R> Stream<L> getLeftOrEmptyStream(final Either<L, R> either) {
        return either.isRight() ?
                Stream.empty() :
                Stream.of(either.left().getOrThrow(() -> noLeftValueException(either)));
    }

    private static <L, R> IllegalStateException noRightValueException(final Either<L, R> either) {
        return new IllegalStateException("Either has no right value although Either#isRight is true: " + either);
    }

    private static <L, R> IllegalStateException noLeftValueException(final Either<L, R> either) {
        return new IllegalStateException("Either has no left value although Either#isLeft is true: " + either);
    }

    /**
     * Creates an {@link Either} from an {@code optional}. If the {@code optional} has a value, we wrap it in a right
     * {@link Either}, otherwise we get a left element using {@code ifAbsent}.
     *
     * @param optional the {@link Optional} we convert to an {@link Either}
     * @param ifAbsent a {@link Supplier} giving us the left either element in case the {@code optional} is empty
     * @param <L>      the type of the left element, provided by {@code ifAbsent}
     * @param <R>      the type of the right element and the type of the value inside the {@code optional}, if it has one
     * @return an {@link Either} created from the {@code optional}
     */
    public static <L, R> Either<L, R> fromOptional(Optional<R> optional, Supplier<L> ifAbsent) {
        return optional.<Either<L, R>>map(Either::right)
                .orElseGet(() -> Either.left(ifAbsent.get()));
    }

    /**
     * Converts an {@link Either} to a {@link CompletableFuture}.
     *
     * @param either             the {@link Either} we convert to a {@link CompletableFuture}
     * @param leftValToThrowable converts the left value of the {@code either} to a throwable if {@code either}.
     *                           We put this throwable into the returned failed {@link CompletableFuture}, if the
     *                           {@code either} is left.
     * @param <L>                the type of the left value inside the {@code either}
     * @param <R>                the type of the right value inside the {@code either}
     * @return if the {@code either} is right, an immediately completed {@link CompletableFuture} containing the
     * {@code either}'s right value. Otherwise, an immediately failed {@link CompletableFuture} containing the
     * {@link Throwable} created from {@code leftValToThrowable}
     */
    public static <L, R> CompletableFuture<R> toFuture(Either<L, R> either, Function<L, Throwable> leftValToThrowable) {

        return either.fold(
                left -> CompletableFuture.failedFuture(leftValToThrowable.apply(left)),
                CompletableFuture::completedFuture);
    }

    /**
     * Gets the right value of an {@code either} if it exists, otherwise throws an exception provided by
     * {@code leftValToEx}.
     * <p>
     * This is handy for unit tests where it's no issue to throw exceptions since they are handled predictably
     * (i.e., the test fails). For production code, use {@link Either#map}, {@link Either#flatMap}, or
     * {@link Either#fold} to get at the right value inside the {@link Either}.
     *
     * @param either      the {@link Either} whose right value we get if it has one
     * @param leftValToEx a function to convert the left value of the {@code either} to a {@link RuntimeException} which
     *                    this method will then throw if {@code either}'s value is left
     * @param <L>         the type of the {@code either}'s left value
     * @param <R>         the type of the {@code either}'s right value
     * @return the right value inside {@code either} if it has one
     * @throws RuntimeException otherwise
     */
    public static <L, R> R getRightOrThrow(Either<L, R> either, Function<L, RuntimeException> leftValToEx) {

        if (either.isRight()) {
            return either.getOrThrow(() -> noRightValueException(either));
        } else {
            L left = either.left().getOrThrow(() -> noLeftValueException(either));
            throw leftValToEx.apply(left);
        }
    }

    /**
     * Converts an {@code either} that has a left value of type {@link Exception} into a {@link Try}.
     *
     * @param either we convert this {@link Either} into a {@link Try}.
     * @param <R>    the type of the {@code either}'s right value
     * @return a {@link Try} containing the {@code either}'s exception if the {@code either} was left, or the
     * {@code either}'s right value
     */
    public static <R> Try<R> toTry(Either<? extends Exception, R> either) {
        return either.fold(
                Try::failure,
                Try::successful);
    }
}
