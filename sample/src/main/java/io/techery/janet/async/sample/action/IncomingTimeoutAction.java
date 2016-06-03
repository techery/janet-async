package io.techery.janet.async.sample.action;

import io.techery.janet.async.annotations.AsyncAction;
import io.techery.janet.async.annotations.Payload;

@AsyncAction(value = "event_never_receive", incoming = true)
public class IncomingTimeoutAction {

    @Payload String body;

    @Override
    public String toString() {
        return "IncomingResponseAction{" +
                "body='" + body + '\'' +
                '}';
    }

}
