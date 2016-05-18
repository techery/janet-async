package io.techery.janet.async.sample.action;

import io.techery.janet.async.PendingResponseMatcher;
import io.techery.janet.async.annotations.AsyncAction;
import io.techery.janet.async.annotations.PendingResponse;
import io.techery.janet.async.annotations.Payload;
import io.techery.janet.async.sample.model.Body;

@AsyncAction(value = "test3", incoming = true)
public class TestTimeoutAction {

    @Payload
    Body data;

    @PendingResponse(value = TestResponseMatcher.class, timeout = 3000)
    public TestTimeoutAction response;

    @Override public String toString() {
        return "TestTimeoutAction{" +
            "data=" + data +
            ", response=" + response +
            '}';
    }

    public static class TestResponseMatcher implements PendingResponseMatcher<TestTimeoutAction, TestTimeoutAction> {

        @Override
        public boolean match(TestTimeoutAction requestAction, TestTimeoutAction response) {
            return true;
        }
    }
}
