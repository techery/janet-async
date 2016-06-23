package io.techery.janet.async.protocol;

import io.techery.janet.async.model.Message;

public interface MessageRule<T> {

    /**
     * Invokes for each incoming message
     *
     * @param message
     * @return T extracted payload
     * @throws Throwable parsing or logic error
     */
    T handleMessage(Message message) throws Throwable;

    Message createMessage(String event, T payload) throws Throwable;

}
