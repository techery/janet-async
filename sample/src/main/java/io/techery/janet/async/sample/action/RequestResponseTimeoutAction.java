package io.techery.janet.async.sample.action;

import io.techery.janet.async.PendingResponseMatcher;
import io.techery.janet.async.annotations.AsyncAction;
import io.techery.janet.async.annotations.Payload;
import io.techery.janet.async.annotations.PendingResponse;
import io.techery.janet.async.sample.model.Body;

@AsyncAction(value = "event_for_timeout")
public class RequestResponseTimeoutAction {

    @Payload public Body data;

    @PendingResponse(value = ResponseMatcher.class, timeout = 3000)
    public IncomingTimeoutAction response;

    @Override public String toString() {
        return "RequestResponseTimeoutAction{" +
            "body=" + data +
            ", response=" + response +
            '}';
    }

    public static class ResponseMatcher implements PendingResponseMatcher<RequestResponseTimeoutAction, IncomingTimeoutAction> {

        @Override
        public boolean match(RequestResponseTimeoutAction requestAction, IncomingTimeoutAction response) {
            return true;
        }
    }
}
