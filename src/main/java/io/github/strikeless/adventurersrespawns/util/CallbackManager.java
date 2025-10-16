package io.github.strikeless.adventurersrespawns.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class CallbackManager<T> {
    private final List<Function<T, Boolean>> listeners = new ArrayList<>();

    public void dispatch(T value) {
        listeners.removeIf(listener -> !listener.apply(value));
    }

    public void registerListener(Function<T, Boolean> listener) {
        listeners.add(listener);
    }

    public void unregisterListener(Function<T, Boolean> listener) {
        listeners.remove(listener);
    }
}
