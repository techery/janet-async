package io.techery.janet.async.sample.action;

import io.techery.janet.async.annotations.AsyncAction;
import io.techery.janet.async.annotations.Payload;

@AsyncAction(value = "event_from_server_to_client_as_response", incoming = true)
public class IncomingResponseAction {

    @Payload String body;

    @Override
    public String toString() {
        return "IncomingResponseAction{" +
                "body='" + body + '\'' +
                '}';
    }

}
