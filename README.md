## AsyncActionService
Socket service for [Janet](https://github.com/techery/janet). Supports diff socket-clients, converters and event customization.

### Getting Started
##### 1. Define service and add it to `Janet`
```java
ActionService asyncService = new AsyncActionService(API_URL, new SocketIO(), new GsonConverter(new Gson()))
Janet janet = new Janet.Builder().addService(asyncService).build();
```

Service requires: end-point url, [AsyncClient](clients) and [Converter](https://github.com/techery/janet-converters).
 
##### 2. Define event action class
```java
@AsyncAction("sample_action_event")
public class SampleAction {

    @Payload
    SampleBody body; // body to send (aka message)

    @PendingResponse(value = ResponseMatcher.class, timeout = 3000)
    AnotherSampleAction response; // response action to wait for

    public SampleAction(SampleBody body) {
        this.body = body;
    }

    static class ResponseMatcher implements PendingResponseMatcher<SampleAction, AnotherSampleAction> {

        @Override
        public boolean match(SampleAction requestAction, AnotherSampleAction response) {
            // Condition to link request with response
            return requestAction.body.id == response.body.id;
        }
    }
}
```
Each action is an individual class that contains all information about the request/response.
It must be annotated with `@AsyncAction`.

Action (aka event) could be:
* sent as request message;
* observed as incoming any time from server (see `incoming = true`);
* sent with pending response pseudo-synchronization (see `@PendingResponse`);

##### 3. Use `ActionPipe` to establish connection and send/observe action
Connection is controlled via system actions:
* `ConnectAsyncAction` used to connect to server;
* `DisconnectAsyncAction` used to disconnect from server;

```java
// define server connection pipe
ActionPipe<ConnectAsyncAction> connectionPipe = janet.createPipe(ConnectAsyncAction.class, Schedulers.io());
connectionPipe.observe().subscribe(new ActionStateSubscriber<ConnectAsyncAction>()
        .onSuccess(connectAsyncAction -> System.out.println("Connected"))
        .onFail((connectAsyncAction, throwable) -> throwable.printStackTrace())
);
// establish connection
connectionPipe.send(new ConnectAsyncAction());
```

Event could be sent regarding connection status:
```java
// send event if/when connected
connectionPipe.observeSuccessWithReplay().first()
    .flatMap(connection -> {
        SampleAction action = new SampleAction(new SampleBody(1, "some_payload"));
        return janet.createPipe(SampleAction.class).createObservable(action);
    })
    .subscribe(new ActionStateSubscriber<SampleAction>()
        .onSuccess(action -> System.out.println("Got response: " + action.response.body))
        .onFail((testAction, throwable) -> throwable.printStackTrace())
    );
```

### AsyncAction Configuration

`@AsyncAction` annotation defines:
* `value` – event name;
* `incoming` – indicates we can receive action any time from server.  

To configure event, annotate fields with:
* `@Payload` – body to be sent/received with event, aka message;
* `@PendingResponse` – pending action from server to sync request with:
    * value – predicate class, defines rule to match request with response;
    * timeout – if no response received, fail status with exception will be thrown.

### Advanced bits
* based on annotation processing;
* supports action inheritance;
* supports request cancelation;
* provides useful `AsyncServiceException` for failed requests;

### Download
```groovy
repositories {
    jcenter()
    maven { url "https://jitpack.io" }
}

dependencies {
    compile 'com.github.techery.janet-async:service:xxx'
    apt     'com.github.techery.janet-async:service-compiler:xxx'
    compile 'com.github.techery.janet-async:client-nkzawa-socket.io:xxx'
    // or compile 'com.github.techery.janet-async:client-socket.io:xxx'
    compile 'com.github.techery.janet-converters:gson:yyy'
    // it is recommended you also explicitly depend on latest Janet version for bug fixes and new features.
    compile 'com.github.techery:janet:zzz' 
}
```
* janet: [![](https://jitpack.io/v/techery/janet.svg)](https://jitpack.io/#techery/janet)
* janet-async: [![](https://jitpack.io/v/techery/janet-async.svg)](https://jitpack.io/#techery/janet-async)
* janet-converters: [![](https://jitpack.io/v/techery/janet-converters.svg)](https://jitpack.io/#techery/janet-converters)

### Proguard
* Add [Rules](service/proguard-rules.pro) to your proguard config.

## License

    Copyright (c) 2016 Techery

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


