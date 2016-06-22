package model;

import io.techery.janet.async.annotations.AsyncAction;
import io.techery.janet.async.annotations.Payload;

@AsyncAction(value = "stub_event")
public class StubAsyncAction {

    @Payload
    Void none;
}
