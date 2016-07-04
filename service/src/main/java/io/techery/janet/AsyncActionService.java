package io.techery.janet;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import io.techery.janet.AsyncActionService.QueuePoller.PollCallback;
import io.techery.janet.async.actions.ConnectAsyncAction;
import io.techery.janet.async.actions.DisconnectAsyncAction;
import io.techery.janet.async.actions.ErrorAsyncAction;
import io.techery.janet.async.annotations.AsyncAction;
import io.techery.janet.async.annotations.Payload;
import io.techery.janet.async.annotations.Response;
import io.techery.janet.async.exception.AsyncServiceException;
import io.techery.janet.async.exception.PendingResponseException;
import io.techery.janet.async.model.Message;
import io.techery.janet.async.model.ProtocolAction;
import io.techery.janet.async.protocol.AsyncProtocol;
import io.techery.janet.body.ActionBody;
import io.techery.janet.converter.Converter;

/**
 * Provide support async protocols. {@link AsyncActionService} performs actions with annotation
 * {@linkplain AsyncAction @AsyncAction}. Every action is async message that contains message data as a field annotated
 * with {@linkplain Payload @Payload}.
 * <p></p>
 * Also {@linkplain AsyncActionService} has algorithm to synchronize outcoming and incoming messages.
 * Action could wait for response and store it to field with annotation {@linkplain Response @Response}.
 * Type of that field must be a class of incoming action. To link action with its response set class in the annotation implemented by
 * PendingResponseMatcher where the condition for matching present.
 */
final public class AsyncActionService extends ActionService {

    static final String ROSTER_CLASS_SIMPLE_NAME = "AsyncActionsRoster";
    private final static String ROSTER_CLASS_NAME = Janet.class.getPackage()
            .getName() + "." + ROSTER_CLASS_SIMPLE_NAME;

    static final String FACTORY_CLASS_SIMPLE_NAME = "AsyncActionWrapperFactoryImpl";
    private final static String FACTORY_CLASS_NAME = Janet.class.getPackage()
            .getName() + "." + FACTORY_CLASS_SIMPLE_NAME;

    private static final String ERROR_GENERATOR = "Something was happened with code generator. Check dependence of janet-async-compiler";

    private final String url;
    private final AsyncClient client;
    private final AsyncProtocol protocol;
    private final Converter converter;

    private final ConcurrentLinkedQueue<ActionHolder<ConnectAsyncAction>> connectActionQueue;
    private final ConcurrentLinkedQueue<ActionHolder<DisconnectAsyncAction>> disconnectActionQueue;
    private AsyncActionsRosterBase actionsRoster;
    private final AsyncActionSynchronizer synchronizer;
    private AsyncActionWrapperFactory actionWrapperFactory;
    private final List<Object> runningActions;


    public AsyncActionService(String url, AsyncClient client, AsyncProtocol protocol, Converter converter) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        if (client == null) {
            throw new IllegalArgumentException("client == null");
        }
        if (protocol == null) {
            throw new IllegalArgumentException("protocol == null");
        }
        if (converter == null) {
            throw new IllegalArgumentException("converter == null");
        }
        this.url = url;
        this.client = client;
        this.protocol = protocol;
        this.converter = converter;
        this.connectActionQueue = new ConcurrentLinkedQueue<ActionHolder<ConnectAsyncAction>>();
        this.disconnectActionQueue = new ConcurrentLinkedQueue<ActionHolder<DisconnectAsyncAction>>();
        this.synchronizer = new AsyncActionSynchronizer(waitingErrorCallback);
        this.runningActions = new CopyOnWriteArrayList<Object>();
        loadActionWrapperFactory();
        loadAsyncActionRooster();
        client.setCallback(clientCallback);
        for (String event : actionsRoster.getRegisteredEvents()) {
            client.subscribe(event);
        }
    }

    @Override protected Class getSupportedAnnotationType() {
        return AsyncAction.class;
    }

    @Override protected <A> void sendInternal(ActionHolder<A> holder) throws AsyncServiceException {
        callback.onStart(holder);
        if (handleConnectionAction(holder)) {
            return;
        }
        try {
            runningActions.add(holder.action());
            AsyncActionWrapper wrapper = getAsyncActionWrapper(holder);
            if (!client.isConnected()) {
                connect(false);
            }
            sendAction(wrapper);
            if (!wrapper.awaitsResponse()) {
                callback.onSuccess(holder);
            }
        } catch (CancelException ignored) {
        } finally {
            runningActions.remove(holder.action());
        }
    }

    @Override protected <A> void cancel(ActionHolder<A> holder) {
        AsyncActionWrapper wrapper = getAsyncActionWrapper(holder);
        runningActions.remove(holder.action());
        if (wrapper.awaitsResponse()) {
            synchronizer.remove(wrapper);
        }
    }

    private void sendAction(AsyncActionWrapper wrapper) throws AsyncServiceException, CancelException {
        try {
            ActionBody payloadBody;
            if (wrapper.isBytesPayload()) {
                payloadBody = (ActionBody) wrapper.getPayload();
            } else {
                payloadBody = converter.toBody(wrapper.getPayload());
            }
            ProtocolAction protocolAction = ProtocolAction.of(wrapper.getEvent())
                    .payload(payloadBody, wrapper.isBytesPayload());
            Message message = protocol.messageRule().createMessage(protocolAction);
            client.send(message);
            if (wrapper.awaitsResponse()) {
                if (protocol.responseMatcher() == null) {
                    throw new JanetInternalException(
                            String.format("Action %s can't be sent because waits response but ResponseMatcher wasn't declared", wrapper.action
                                    .getClass()
                                    .getSimpleName()));
                }
                wrapper.setProtocolAction(protocolAction);
                synchronizer.put(wrapper);
            }
            throwIfCanceled(wrapper.action);
        } catch (CancelException e) {
            throw e;
        } catch (Throwable t) {
            throw new AsyncServiceException(t);
        }
    }

    private void throwIfCanceled(Object action) throws CancelException {
        if (!runningActions.contains(action)) {
            throw new CancelException();
        }
    }

    @SuppressWarnings("unchecked")
    private <A> boolean handleConnectionAction(ActionHolder<A> holder) throws AsyncServiceException {
        A action = holder.action();
        if (action instanceof ConnectAsyncAction) {
            ConnectAsyncAction connectAsyncAction = (ConnectAsyncAction) action;
            if (client.isConnected() && !connectAsyncAction.reconnectIfConnected) {
                callback.onSuccess(holder);
            }
            connect(connectAsyncAction.reconnectIfConnected);
            connectActionQueue.add((ActionHolder<ConnectAsyncAction>) holder);
            return true;
        }
        if (action instanceof DisconnectAsyncAction) {
            try {
                client.disconnect();
            } catch (Throwable t) {
                throw new AsyncServiceException(t);
            }
            disconnectActionQueue.add((ActionHolder<DisconnectAsyncAction>) holder);
            return true;
        }
        return false;
    }

    private void connect(boolean reconnectIfConnected) throws AsyncServiceException {
        try {
            client.connect(url, reconnectIfConnected);
        } catch (Throwable t) {
            throw new AsyncServiceException(t);
        }
    }

    private void onMessageReceived(Message message) {
        Throwable protocolError = null;
        ProtocolAction protocolAction = null;
        try {
            protocolAction = protocol.messageRule().handleMessage(message);
            if (protocolAction == null) {
                throw new NullPointerException("MessageRule.handleMessage returned null");
            }
        } catch (Throwable throwable) {
            protocolError = throwable;
        }
        processReceivedAction(protocolAction, protocolError);
        syncReceivedAction(protocolAction, protocolError);
    }

    private void processReceivedAction(ProtocolAction protocolAction, Throwable protocolError) {
        List<Class> actionClasses = actionsRoster.getActionClasses(protocolAction.getEvent());
        for (Class actionClass : actionClasses) {
            ActionHolder holder = ActionHolder.create((createActionInstance(actionClass)));
            try {
                if (protocolError != null) {
                    throw protocolError;
                }
                AsyncActionWrapper actionWrapper = getAsyncActionWrapper(holder);
                actionWrapper.setPayload(converter.fromBody(protocolAction.getPayload(), actionWrapper.getPayloadFieldType()));
                callback.onSuccess(holder);
            } catch (Throwable throwable) {
                callback.onFail(holder, new AsyncServiceException(protocolError));
            }
        }
    }

    private void syncReceivedAction(final ProtocolAction protocolAction, Throwable protocolError) {
        for (AsyncActionWrapper wrapper : synchronizer.sync(new AsyncActionSynchronizer.Callback() {
            @Override public boolean call(AsyncActionWrapper wrapper) {
                return protocol.responseMatcher().match(wrapper.protocolAction, protocolAction);
            }
        })) {
            try {
                if (protocolError != null) {
                    throw protocolError;
                }
                wrapper.setResponse(converter.fromBody(protocolAction.getPayload(), wrapper.getResponseFieldType()));
                callback.onSuccess(wrapper.holder);
            } catch (Throwable t) {
                callback.onFail(wrapper.holder, new AsyncServiceException(t));
            }
        }
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final AsyncClient.Callback clientCallback = new AsyncClient.Callback() {

        private QueuePoller queuePoller = new QueuePoller();

        @Override public void onConnect() {
            queuePoller.poll(connectActionQueue, new PollCallback<ActionHolder<ConnectAsyncAction>>() {
                @SuppressWarnings("unchecked")
                @Override public ActionHolder<ConnectAsyncAction> createIfEmpty() {
                    return ActionHolder.create(new ConnectAsyncAction());
                }

                @Override public void onNext(ActionHolder<ConnectAsyncAction> item) {
                    callback.onSuccess(item);
                }
            });
        }

        @Override public void onDisconnect(String reason) {
            queuePoller.poll(disconnectActionQueue, new PollCallback<ActionHolder<DisconnectAsyncAction>>() {
                @SuppressWarnings("unchecked")
                @Override public ActionHolder<DisconnectAsyncAction> createIfEmpty() {
                    return ActionHolder.create(new DisconnectAsyncAction());
                }

                @Override public void onNext(ActionHolder<DisconnectAsyncAction> item) {
                    callback.onSuccess(item);
                }
            });
        }

        @Override public void onConnectionError(final Throwable t) {
            queuePoller.poll(connectActionQueue, new PollCallback<ActionHolder<ConnectAsyncAction>>() {
                @SuppressWarnings("unchecked")
                @Override public ActionHolder<ConnectAsyncAction> createIfEmpty() {
                    return ActionHolder.create(new ConnectAsyncAction());
                }

                @Override public void onNext(ActionHolder<ConnectAsyncAction> item) {
                    callback.onFail(item, new AsyncServiceException("ConnectionError", t));
                }
            });
        }

        @Override public void onError(Throwable t) {
            callback.onFail(ActionHolder.create(new ErrorAsyncAction(t)), new AsyncServiceException("Server sent error", t));
        }

        @Override public void onMessage(Message message) {
            onMessageReceived(message);
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final AsyncActionSynchronizer.OnCleanedListener waitingErrorCallback = new AsyncActionSynchronizer.OnCleanedListener() {
        @Override
        public void onCleaned(AsyncActionWrapper wrapper, Reason reason) {
            PendingResponseException exception = null;
            switch (reason) {
                case TIMEOUT: {
                    exception = PendingResponseException.forTimeout(wrapper.getResponseTimeout());
                    break;
                }
                case LIMIT: {
                    exception = PendingResponseException.forLimit(AsyncActionSynchronizer.PENDING_ACTIONS_EVENT_LIMIT);
                    break;
                }
            }
            callback.onFail(wrapper.holder, new AsyncServiceException("Action " + wrapper.action + " hasn't got a response.", exception));
        }
    };

    private <A> AsyncActionWrapper getAsyncActionWrapper(ActionHolder<A> holder) {
        AsyncActionWrapper wrapper = actionWrapperFactory.make(holder);
        if (wrapper == null) {
            throw new JanetInternalException(ERROR_GENERATOR);
        }
        return wrapper;
    }

    @SuppressWarnings("unchecked")
    private void loadActionWrapperFactory() {
        try {
            Class<? extends AsyncActionWrapperFactory> clazz
                    = (Class<? extends AsyncActionWrapperFactory>) Class.forName(FACTORY_CLASS_NAME);
            actionWrapperFactory = clazz.newInstance();
        } catch (Exception e) {
            throw new JanetInternalException(ERROR_GENERATOR);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadAsyncActionRooster() {
        try {
            Class<? extends AsyncActionsRosterBase> clazz
                    = (Class<? extends AsyncActionsRosterBase>) Class.forName(ROSTER_CLASS_NAME);
            actionsRoster = clazz.newInstance();
        } catch (Exception e) {
            throw new JanetInternalException(ERROR_GENERATOR);
        }
    }

    private Object createActionInstance(Class aClass) {
        try {
            return aClass.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    interface AsyncActionWrapperFactory {
        AsyncActionWrapper make(ActionHolder holder);
    }

    static class QueuePoller {
        <U> void poll(Queue<U> q, PollCallback<U> callback) {
            do {
                U item = q.poll();
                if (item == null) {
                    item = callback.createIfEmpty();
                }
                callback.onNext(item);
            } while (q.peek() != null);
        }

        interface PollCallback<T> {
            void onNext(T item);
            T createIfEmpty();
        }
    }
}
