package io.techery.janet.async.protocol;

import io.techery.janet.async.model.Message;

public interface PayloadConverter<T> {

    T extractPayload(Message message) throws Throwable;

    Message createMessage(String event, T payload) throws Throwable;

}
