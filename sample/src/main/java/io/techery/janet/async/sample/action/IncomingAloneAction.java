package io.techery.janet.async.sample.action;

import io.techery.janet.async.annotations.AsyncAction;
import io.techery.janet.async.annotations.Payload;
import io.techery.janet.async.sample.model.Body;

@AsyncAction(value = "event_from_server_to_client", incoming = true)
public class IncomingAloneAction {

    @Payload Body body;

    @Override
    public String toString() {
        return "IncomingAloneAction{" +
                "body='" + body + '\'' +
                '}';
    }

}
