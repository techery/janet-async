package io.techery.janet.async.sample.action;

import io.techery.janet.async.annotations.AsyncAction;
import io.techery.janet.async.annotations.Payload;
import io.techery.janet.async.sample.model.Body;

@AsyncAction(value = "event_never_receive", incoming = true)
public class IncomingTimeoutAction {

    @Payload Body body;

    @Override
    public String toString() {
        return "IncomingResponseAction{" +
                "body='" + body + '\'' +
                '}';
    }

}
