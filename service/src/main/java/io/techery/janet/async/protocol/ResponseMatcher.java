package io.techery.janet.async.protocol;

import io.techery.janet.async.model.ProtocolAction;

public interface ResponseMatcher {

    boolean match(ProtocolAction waitingAction, ProtocolAction incomingAction);

}
