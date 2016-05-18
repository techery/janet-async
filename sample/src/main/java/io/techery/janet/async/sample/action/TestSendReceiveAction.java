package io.techery.janet.async.sample.action;

import io.techery.janet.async.PendingResponseMatcher;
import io.techery.janet.async.annotations.AsyncAction;
import io.techery.janet.async.annotations.PendingResponse;
import io.techery.janet.async.annotations.Payload;
import io.techery.janet.async.sample.model.Body;

@AsyncAction(value = "test1", incoming = true)
public class TestSendReceiveAction {

    @Payload
    public Body body;

    @PendingResponse(value = ResponseMatcher.class, timeout = 3000)
    public TestSendReceiveAction response;

    @Override
    public String toString() {
        return "TestSendReceiveAction{" +
                "body=" + body +
                ", response=" + response +
                '}';
    }

    static class ResponseMatcher implements PendingResponseMatcher<TestSendReceiveAction, TestSendReceiveAction> {

        @Override
        public boolean match(TestSendReceiveAction requestAction, TestSendReceiveAction response) {
            return requestAction.body.id == response.body.id;
        }
    }
}
