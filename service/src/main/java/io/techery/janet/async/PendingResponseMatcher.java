package io.techery.janet.async;

public interface PendingResponseMatcher<T, R> {

    boolean match(T requestAction, R response);
}
