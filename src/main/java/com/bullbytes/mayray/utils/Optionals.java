package com.bullbytes.mayray.utils;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Helps with {@link Optional}s.
 * <p>
 * Person of contact: Matthias Braun
 */
public final class Optionals {
    private Optionals() {}

    /**
     * Applies {@code ifPresent} to the value of {@code optional} if it's present. Otherwise, calls {@code ifAbsent}.
     *
     * @param optional  the {@link Optional} whose value is passed to {@code ifPresent} if it's present
     * @param ifAbsent  if {@code optional}'s value is absent, we call this {@link Supplier} to produce a value of type
     *                  {@code Res}
     * @param ifPresent the {@link Function} to which we pass the value of {@code optional} to produce a value
     *                  of type {@code Res}
     * @param <T>       the type of {@code optional}'s value
     * @param <Res>     the type of the value produced by both {@code ifAbsent} and {@code ifPresent}
     * @return a value of type {@code Res}
     */
    public static <T, Res> Res fold(Optional<T> optional,
                                    Supplier<Res> ifAbsent,
                                    Function<T, Res> ifPresent) {

        return optional.isPresent() ?
                ifPresent.apply(optional.get()) :
                ifAbsent.get();
    }
}
