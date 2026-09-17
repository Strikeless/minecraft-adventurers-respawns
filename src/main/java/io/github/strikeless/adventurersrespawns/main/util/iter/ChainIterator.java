package io.github.strikeless.adventurersrespawns.main.util.iter;

import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Iterator;

public class ChainIterator<T> implements Iterator<T> {
    private final ArrayDeque<Iterator<T>> innerIterators;

    @SafeVarargs
    public ChainIterator(Iterator<T>... innerIterators) {
        this.innerIterators = new ArrayDeque<>();
        Collections.addAll(this.innerIterators, innerIterators);
    }

    @Override
    public boolean hasNext() {
        var currentInnerIterator = this.getFirstNonEmptyInnerIterator();
        if (currentInnerIterator == null) return false;
        return currentInnerIterator.hasNext();
    }

    @Override
    public T next() {
        var currentInnerIterator = this.getFirstNonEmptyInnerIterator();
        if (currentInnerIterator == null) return null;
        return currentInnerIterator.next();
    }

    private @Nullable Iterator<T> getFirstNonEmptyInnerIterator() {
        var currentInnerIterator = this.innerIterators.peekFirst();

        // While the current inner iterator is empty, advance to the next one if one exists.
        while (currentInnerIterator != null && !currentInnerIterator.hasNext()) {
            this.innerIterators.removeFirst();
            currentInnerIterator = this.innerIterators.peekFirst();
        }

        return currentInnerIterator;
    }
}
