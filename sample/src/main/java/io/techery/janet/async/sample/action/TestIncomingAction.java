package io.techery.janet.async.sample.action;

import io.techery.janet.async.annotations.AsyncAction;
import io.techery.janet.async.annotations.AsyncMessage;
import io.techery.janet.async.sample.model.Body;

@AsyncAction(value = "test2", incoming = true)
public class TestIncomingAction {

    @AsyncMessage
    Body data;

    @Override
    public String toString() {
        return "TestIncomingAction{" +
                "data='" + data + '\'' +
                '}';
    }

}
