package io.github.strikeless.adventurersrespawns.main.util.iter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.Optional;

public class Iterators {
    @SafeVarargs
    public static <T> ChainIterator<T> chain(Iterator<T>... innerIterators) {
        return new ChainIterator<>(innerIterators);
    }

    public static <T> OnceIterator<T> once(@Nullable T value) {
        return OnceIterator.of(value);
    }

    public static <T> OnceIterator<T> onceOrNever(@Nullable T value) {
        return OnceIterator.ofOrNever(value);
    }

    public static <T> OnceIterator<T> onceOrNever(@NonNull Optional<T> value) {
        return OnceIterator.ofOrNever(value);
    }
}
