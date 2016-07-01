package model;

import io.techery.janet.async.annotations.Payload;

public class BaseStubAsyncAction<T> {

    @Payload
    T payload;

}
