package io.techery.janet.async.sample.action;

import io.techery.janet.async.SyncPredicate;
import io.techery.janet.async.annotations.AsyncAction;
import io.techery.janet.async.annotations.AsyncMessage;
import io.techery.janet.async.annotations.SyncedResponse;
import io.techery.janet.async.sample.model.Body;

@AsyncAction(value = "test3", incoming = true)
public class TestTimeoutAction {

    @AsyncMessage
    Body data;

    @SyncedResponse(value = TestTimeoutAction.TestSyncPredicate.class, timeout = 3000)
    public TestTimeoutAction response;

    @Override public String toString() {
        return "TestTimeoutAction{" +
            "data=" + data +
            ", response=" + response +
            '}';
    }

    public static class TestSyncPredicate implements SyncPredicate<TestTimeoutAction, TestTimeoutAction> {

        @Override
        public boolean isResponse(TestTimeoutAction requestAction, TestTimeoutAction response) {
            return true;
        }
    }
}
