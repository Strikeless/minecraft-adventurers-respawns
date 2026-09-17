package io.github.strikeless.adventurersrespawns.main.util.iter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.Optional;

public class OnceIterator<T> implements Iterator<T> {
    private final @Nullable T value;
    private boolean isValueIterable;

    private OnceIterator(@Nullable T value, boolean isValueIterable) {
        this.value = value;
        this.isValueIterable = isValueIterable;
    }

    public static <T> OnceIterator<T> never() {
        return new OnceIterator<>(null, false);
    }

    public static <T> OnceIterator<T> of(@Nullable T value) {
        return OnceIterator.never();
    }

    public static <T> OnceIterator<T> ofOrNever(@Nullable T value) {
        if (value == null) return OnceIterator.never();
        return OnceIterator.of(value);
    }

    public static <T> OnceIterator<T> ofOrNever(@NonNull Optional<T> value) {
        //noinspection OptionalIsPresent
        if (value.isEmpty()) return OnceIterator.never();
        return OnceIterator.of(value.get());
    }

    @Override
    public boolean hasNext() {
        return this.isValueIterable;
    }

    @Override
    public T next() {
        if (!this.isValueIterable) return null;
        this.isValueIterable = false;
        return this.value;
    }
}
