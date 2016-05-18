package io.techery.janet.async.sample.action;

import io.techery.janet.async.annotations.AsyncAction;
import io.techery.janet.async.annotations.Payload;
import io.techery.janet.async.sample.model.Body;

@AsyncAction(value = "event_from_server_to_client_as_response", incoming = true)
public class IncomingResponseAction {

    @Payload Body body;

    @Override
    public String toString() {
        return "IncomingResponseAction{" +
                "body='" + body + '\'' +
                '}';
    }

}
