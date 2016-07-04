package io.techery.janet.async.sample;

import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;

import io.techery.janet.ActionPipe;
import io.techery.janet.AsyncActionService;
import io.techery.janet.Janet;
import io.techery.janet.async.actions.ConnectAsyncAction;
import io.techery.janet.async.model.Message;
import io.techery.janet.async.model.ProtocolAction;
import io.techery.janet.async.protocol.AsyncProtocol;
import io.techery.janet.async.protocol.MessageRule;
import io.techery.janet.async.protocol.ResponseMatcher;
import io.techery.janet.async.sample.action.IncomingAloneAction;
import io.techery.janet.async.sample.action.RequestResponseAction;
import io.techery.janet.async.sample.action.RequestResponseTimeoutAction;
import io.techery.janet.gson.GsonConverter;
import io.techery.janet.helper.ActionStateSubscriber;
import io.techery.janet.nkzawa.SocketIO;
import rx.schedulers.Schedulers;

public class AsyncSample {

    private static String KEY_ID = "id";
    private static String KEY_DATA = "data";

    // 1
    private static final MessageRule MESSAGE_RULE = new MessageRule() {

        private AtomicInteger id = new AtomicInteger();

        @Override public ProtocolAction handleMessage(Message message) throws Throwable {
            String text = message.getDataAsText();
            JSONObject json = new JSONObject(text);
            return ProtocolAction.of(message)
                    .payload(json.getString(KEY_DATA))
                    .metadata(KEY_ID, json.getInt(KEY_ID));
        }

        @Override public Message createMessage(ProtocolAction action) throws Throwable {
            JSONObject json = new JSONObject();
            json.put(KEY_ID, id.incrementAndGet());
            json.put(KEY_DATA, action.getPayloadAsString());
            //save id to metadata for response matching
            action.metadata(KEY_ID, id.get());
            return Message.createTextMessage(action.getEvent(), json.toString());
        }
    };

    private static final ResponseMatcher responseMatcher = new ResponseMatcher() {
        @Override public boolean match(ProtocolAction waitingAction, ProtocolAction incomingAction) {
            if (!waitingAction.isBinaryPayload()
                    && waitingAction.getEvent().equals("event_from_client_to_server")
                    && incomingAction.getEvent().equals("event_from_server_to_client_as_response")) {
                return waitingAction.getMetadata(KEY_ID).equals(incomingAction.getMetadata(KEY_ID));
            }
            return false;
        }
    };

    public static void main(String... args) throws Exception {
        AsyncProtocol protocol = new AsyncProtocol.Builder()
                .setMessageRule(MESSAGE_RULE)
                .setResponseMatcher(responseMatcher)
                .build();

        Janet janet = new Janet.Builder()
                .addService(new AsyncActionService("http://localhost:3000", new SocketIO(), protocol, new GsonConverter(new Gson())))
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
                    action.body = "client_data";
                    sendReceivePipe.send(action);
                });

        connectionPipe.observeSuccess()
                .subscribe(ActionPipe -> {
                    RequestResponseTimeoutAction action = new RequestResponseTimeoutAction();
                    action.data = "client_data";
                    //                    timeoutPipe.send(action);
                });

        // Establish connection

        connectionPipe.send(new ConnectAsyncAction());

        synchronized (Thread.currentThread()) {
            Thread.currentThread().wait();
        }
    }
}
