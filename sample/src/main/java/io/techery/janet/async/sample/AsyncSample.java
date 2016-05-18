package io.techery.janet.async.sample;

import com.google.gson.Gson;

import io.techery.janet.ActionPipe;
import io.techery.janet.AsyncActionService;
import io.techery.janet.Janet;
import io.techery.janet.async.actions.ConnectAsyncAction;
import io.techery.janet.async.sample.action.IncomingAloneAction;
import io.techery.janet.async.sample.action.RequestResponseAction;
import io.techery.janet.async.sample.action.RequestResponseTimeoutAction;
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

        ActionPipe<RequestResponseAction> sendReceivePipe = janet.createPipe(RequestResponseAction.class);
        sendReceivePipe.observe().subscribe(new ActionStateSubscriber<RequestResponseAction>()
                .onSuccess(System.out::println)
                .onFail((testAction, throwable) -> throwable.printStackTrace())
        );

        ActionPipe<IncomingAloneAction> incomingMessagePipe = janet.createPipe(IncomingAloneAction.class);
        incomingMessagePipe.observe().subscribe(new ActionStateSubscriber<IncomingAloneAction>()
                .onSuccess(action -> System.out.println(action))
                .onFail((testFailAction, throwable) -> throwable.printStackTrace())
        );

        ActionPipe<RequestResponseTimeoutAction> timeoutPipe = janet.createPipe(RequestResponseTimeoutAction.class);
        timeoutPipe.observe().subscribe(new ActionStateSubscriber<RequestResponseTimeoutAction>()
                .onSuccess(action -> System.out.println(action))
                .onFail((testFailAction, throwable) -> throwable.printStackTrace())
        );

        // Connect messages pipes to connection

        connectionPipe.observeSuccess()
                .subscribe(ActionPipe -> {
                    RequestResponseAction action = new RequestResponseAction();
                    action.body = new Body();
                    action.body.id = 1;
                    action.body.data = "client_data";
                    sendReceivePipe.send(action);
                });

        connectionPipe.observeSuccess()
                .subscribe(ActionPipe -> {
                    RequestResponseTimeoutAction action = new RequestResponseTimeoutAction();
                    action.data = new Body();
                    action.data.id = 100500;
                    action.data.data = "client_data";
                    timeoutPipe.send(action);
                });

        // Establish connection

        connectionPipe.send(new ConnectAsyncAction());

        synchronized (Thread.currentThread()) {
            Thread.currentThread().wait();
        }
    }
}
