package io.techery.janet.async.sample.action;

import io.techery.janet.async.SyncPredicate;
import io.techery.janet.async.annotations.AsyncAction;
import io.techery.janet.async.annotations.AsyncMessage;
import io.techery.janet.async.annotations.SyncedResponse;
import io.techery.janet.async.sample.model.Body;

@AsyncAction(value = "test1", incoming = true)
public class TestSendReceiveAction {

    @AsyncMessage
    public Body body;

    @SyncedResponse(value = TestSyncPredicate.class, timeout = 3000)
    public TestSendReceiveAction response;

    @Override
    public String toString() {
        return "TestSendReceiveAction{" +
                "body=" + body +
                ", response=" + response +
                '}';
    }

    public static class TestSyncPredicate implements SyncPredicate<TestSendReceiveAction, TestSendReceiveAction> {

        @Override
        public boolean isResponse(TestSendReceiveAction requestAction, TestSendReceiveAction response) {
            return requestAction.body.id == response.body.id;
        }
    }
}
