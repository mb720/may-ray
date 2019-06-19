package com.bullbytes.mayray.utils;

import io.vavr.control.Either;

/**
 * Deals with the type of an object and conversions to other types.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum TypeUtil {
    ;

    /**
     * Casts an {@code objectToCast} to type {@code targetType}.
     * If the cast fails, we return a {@link FailMessage}.
     *
     * @param targetType   contains the type to which we want to cast the {@code objectToCast}
     * @param objectToCast we want to cast this object to an object of type {@code T}
     * @param <T>          the type to which we want to cast the {@code objectToCast}
     * @return {@link Either either} the {@code objectToCast} as an object of type {@code T} or a
     * {@link FailMessage} if the cast failed
     */
    public static <T> Either<FailMessage, T> castTo(Class<T> targetType, Object objectToCast) {

        Either<FailMessage, T> result;
        if (targetType.isInstance(objectToCast)) {
            result = Either.right(targetType.cast(objectToCast));
        } else {
            if (objectToCast == null) {
                result = Either.left(FailMessage.create("Can't cast null object"));
            } else {
                FailMessage msg =
                        FailMessage.formatted("Can't cast object to %s since its class is %s", targetType, objectToCast.getClass());
                result = Either.left(msg);
            }
        }
        return result;
    }
}
