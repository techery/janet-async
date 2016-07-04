package io.techery.janet.async.protocol;

import io.techery.janet.async.model.Message;
import io.techery.janet.async.model.ProtocolAction;

public interface MessageRule {

    /**
     * Invokes for each incoming message
     *
     * @param message
     * @return T extracted payload
     * @throws Throwable parsing or logic error
     */
    ProtocolAction handleMessage(Message message) throws Throwable;

    Message createMessage(ProtocolAction payload) throws Throwable;

}
