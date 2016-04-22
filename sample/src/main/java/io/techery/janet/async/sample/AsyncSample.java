package io.techery.janet.async.sample;

import com.google.gson.Gson;

import io.techery.janet.ActionPipe;
import io.techery.janet.AsyncActionService;
import io.techery.janet.Janet;
import io.techery.janet.async.actions.ConnectAsyncAction;
import io.techery.janet.async.sample.action.TestIncomingAction;
import io.techery.janet.async.sample.action.TestSendReceiveAction;
import io.techery.janet.async.sample.action.TestTimeoutAction;
import io.techery.janet.async.sample.model.Body;
import io.techery.janet.gson.GsonConverter;
import io.techery.janet.helper.ActionStateSubscriber;
import io.techery.janet.nkzawa.SocketIO;
import rx.schedulers.Schedulers;

public class AsyncSample {

    public static void main(String... args) throws Exception {
        Janet janet = new Janet.Builder()
                .addService(new AsyncActionService("http://localhost:3000", new SocketIO(), new GsonConverter(new Gson())))
                .build();

        // Create connection pipe

        ActionPipe<ConnectAsyncAction> connectionPipe = janet.createPipe(ConnectAsyncAction.class, Schedulers.io());
        connectionPipe.observe()
                .subscribe(new ActionStateSubscriber<ConnectAsyncAction>()
                        .onSuccess(connectAsyncAction -> System.out.println("Connected"))
                        .onFail((connectAsyncAction, throwable) -> throwable.printStackTrace())
                );

        // Create pipes for messages

        ActionPipe<TestSendReceiveAction> sendReceivePipe = janet.createPipe(TestSendReceiveAction.class);
        sendReceivePipe.observe().subscribe(new ActionStateSubscriber<TestSendReceiveAction>()
                .onSuccess(System.out::println)
                .onFail((testAction, throwable) -> throwable.printStackTrace())
        );

        ActionPipe<TestIncomingAction> incomingMessagePipe = janet.createPipe(TestIncomingAction.class);
        incomingMessagePipe.observe().subscribe(new ActionStateSubscriber<TestIncomingAction>()
                .onSuccess(action -> System.out.println(action))
                .onFail((testFailAction, throwable) -> throwable.printStackTrace())
        );

        ActionPipe<TestTimeoutAction> timeoutPipe = janet.createPipe(TestTimeoutAction.class);
        timeoutPipe.observe().subscribe(new ActionStateSubscriber<TestTimeoutAction>()
                .onSuccess(action -> System.out.println(action))
                .onFail((testFailAction, throwable) -> throwable.printStackTrace())
        );

        // Connect messages pipes to connection

        connectionPipe.observeSuccess()
                .subscribe(ActionPipe -> {
                    TestSendReceiveAction action = new TestSendReceiveAction();
                    action.body = new Body();
                    action.body.id = 1;
                    action.body.data = "test";
                    sendReceivePipe.send(action);
                });

        connectionPipe.observeSuccess()
                .subscribe(ActionPipe -> {
                    timeoutPipe.send(new TestTimeoutAction());
                });

        // Establish connection

        connectionPipe.send(new ConnectAsyncAction());

        synchronized (Thread.currentThread()) {
            Thread.currentThread().wait();
        }
    }
}
