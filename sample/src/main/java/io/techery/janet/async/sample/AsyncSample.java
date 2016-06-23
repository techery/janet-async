package io.techery.janet.async.sample;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;

import io.techery.janet.ActionPipe;
import io.techery.janet.AsyncActionService;
import io.techery.janet.Janet;
import io.techery.janet.async.protocol.AsyncProtocol;
import io.techery.janet.async.model.IncomingMessage;
import io.techery.janet.async.model.Message;
import io.techery.janet.async.protocol.MessageRule;
import io.techery.janet.async.protocol.ResponseMatcher;
import io.techery.janet.async.model.WaitingAction;
import io.techery.janet.async.actions.ConnectAsyncAction;
import io.techery.janet.async.sample.action.IncomingAloneAction;
import io.techery.janet.async.sample.action.RequestResponseAction;
import io.techery.janet.async.sample.action.RequestResponseTimeoutAction;
import io.techery.janet.gson.GsonConverter;
import io.techery.janet.helper.ActionStateSubscriber;
import io.techery.janet.nkzawa.SocketIO;
import rx.schedulers.Schedulers;

public class AsyncSample {


    // 1
    private static final MessageRule<String> MESSAGE_RULE = new MessageRule<String>() {

        private AtomicInteger id = new AtomicInteger();

        @Override public String handleMessage(Message message) throws Throwable {
            String text = message.getDataAsText();
            JSONObject json = new JSONObject(text);
            return json.getString("data");
        }

        @Override public Message createMessage(String event, String payload) throws Throwable {
            JSONObject json = new JSONObject();
            json.put("id", id.incrementAndGet());
            json.put("data", payload);
            return Message.createTextMessage(event, json.toString());
        }
    };

    private static final ResponseMatcher responseMatcher = new ResponseMatcher() {
        @Override public boolean match(WaitingAction waitingAction, IncomingMessage incomingMessage) {
            if (!waitingAction.isBinaryPayload()
                    && waitingAction.getMessage().getEvent().equals("event_from_client_to_server")
                    && incomingMessage.getMessage().getEvent().equals("event_from_server_to_client_as_response")) {
                try {
                    JSONObject requestJson = new JSONObject(waitingAction.getMessage().getDataAsText());
                    JSONObject responseJson = new JSONObject(incomingMessage.getMessage().getDataAsText());
                    return requestJson.getInt("id") == responseJson.getInt("id");
                } catch (JSONException e) {
                    return false;
                }
            }
            return false;
        }
    };

    //

    //2
    private static final MessageRule<String> MESSAGE_RULE_2 = new MessageRule<String>() {

        private AtomicInteger id = new AtomicInteger();

        @Override public String handleMessage(Message message) throws Throwable {
            String text = message.getDataAsText();
            JSONObject json = new JSONObject(text);
            return json.getString("data");
        }

        @Override public Message createMessage(String event, String payload) throws Throwable {
            JSONObject json = new JSONObject();
            json.put("id", id.incrementAndGet());
            json.put("data", payload);
            return Message.createTextMessage(event, json.toString());
        }
    };

    private static final ResponseMatcher responseMatcher2 = new ResponseMatcher() {
        @Override public boolean match(WaitingAction waitingAction, IncomingMessage incomingMessage) {
            if (!waitingAction.isBinaryPayload()
                    && waitingAction.getMessage().getEvent().equals("event_from_client_to_server")
                    && incomingMessage.getMessage().getEvent().equals("event_from_server_to_client_as_response")) {
                try {
                    JSONObject requestJson = new JSONObject(waitingAction.getMessage().getDataAsText());
                    JSONObject responseJson = new JSONObject(incomingMessage.getMessage().getDataAsText());
                    return requestJson.getInt("id") == responseJson.getInt("id");
                } catch (JSONException e) {
                    return false;
                }
            }
            return false;
        }
    };

    public static void main(String... args) throws Exception {
        AsyncProtocol protocol = new AsyncProtocol.Builder()
                .setTextMessageRule(MESSAGE_RULE)
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
