package io.techery.janet.async.sample.action;

import io.techery.janet.async.annotations.AsyncAction;
import io.techery.janet.async.annotations.Payload;
import io.techery.janet.async.sample.model.Body;

@AsyncAction(value = "test2", incoming = true)
public class TestIncomingAction {

    @Payload
    Body data;

    @Override
    public String toString() {
        return "TestIncomingAction{" +
                "data='" + data + '\'' +
                '}';
    }

}
