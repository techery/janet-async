package io.techery.janet.async.sample.action;

import io.techery.janet.async.annotations.AsyncAction;
import io.techery.janet.async.annotations.Payload;
import io.techery.janet.async.annotations.Response;

@AsyncAction(value = "event_for_timeout")
public class RequestResponseTimeoutAction {

    @Payload public String data;

    @Response(timeout = 3000)
    String response;

    @Override public String toString() {
        return "RequestResponseTimeoutAction{" +
                "body=" + data +
                ", response=" + response +
                '}';
    }
}
