package io.techery.janet.async.sample.action;

import io.techery.janet.async.annotations.AsyncAction;
import io.techery.janet.async.annotations.Payload;
import io.techery.janet.async.annotations.Response;

@AsyncAction(value = "event_from_client_to_server")
public class RequestResponseAction {

    @Payload public String body;

    @Response(timeout = 3000) public String response;

    @Override
    public String toString() {
        return "RequestResponseAction{" +
                "body=" + body +
                ", response=" + response +
                '}';
    }
}
