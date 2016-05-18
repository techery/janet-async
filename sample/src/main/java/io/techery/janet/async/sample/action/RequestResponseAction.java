package io.techery.janet.async.sample.action;

import io.techery.janet.async.PendingResponseMatcher;
import io.techery.janet.async.annotations.AsyncAction;
import io.techery.janet.async.annotations.PendingResponse;
import io.techery.janet.async.annotations.Payload;
import io.techery.janet.async.sample.model.Body;

@AsyncAction(value = "event_from_client_to_server")
public class RequestResponseAction {

    @Payload public Body body;

    @PendingResponse(value = ResponseMatcher.class, timeout = 3000)
    public IncomingResponseAction response;

    @Override
    public String toString() {
        return "RequestResponseAction{" +
                "body=" + body +
                ", response=" + response +
                '}';
    }

    static class ResponseMatcher implements PendingResponseMatcher<RequestResponseAction, IncomingResponseAction> {

        @Override
        public boolean match(RequestResponseAction requestAction, IncomingResponseAction response) {
            return requestAction.body.id == response.body.id;
        }
    }
}
