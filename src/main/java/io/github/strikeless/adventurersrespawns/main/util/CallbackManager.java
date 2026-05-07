package io.github.strikeless.adventurersrespawns.main.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class CallbackManager<T> {
    private final List<Function<T, Boolean>> listeners = new ArrayList<>();

    public void dispatch(T value) {
        this.listeners.removeIf(listener -> !listener.apply(value));
    }

    public void registerListener(Function<T, Boolean> listener) {
        this.listeners.add(listener);
    }

    public void unregisterListener(Function<T, Boolean> listener) {
        this.listeners.remove(listener);
    }
}
