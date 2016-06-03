package io.techery.janet.async.protocol;

import io.techery.janet.async.model.IncomingMessage;
import io.techery.janet.async.model.WaitingAction;

public interface ResponseMatcher {

    boolean match(WaitingAction waitingAction, IncomingMessage incomingMessage);

}
