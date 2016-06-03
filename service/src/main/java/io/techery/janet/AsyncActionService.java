package io.techery.janet;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import io.techery.janet.AsyncActionService.QueuePoller.PollCallback;
import io.techery.janet.async.PendingResponseMatcher;
import io.techery.janet.async.actions.ConnectAsyncAction;
import io.techery.janet.async.actions.DisconnectAsyncAction;
import io.techery.janet.async.actions.ErrorAsyncAction;
import io.techery.janet.async.annotations.AsyncAction;
import io.techery.janet.async.annotations.PendingResponse;
import io.techery.janet.async.annotations.Payload;
import io.techery.janet.async.exception.AsyncServiceException;
import io.techery.janet.async.exception.PendingResponseException;
import io.techery.janet.body.ActionBody;
import io.techery.janet.body.BytesArrayBody;
import io.techery.janet.body.StringBody;
import io.techery.janet.converter.Converter;
import io.techery.janet.converter.ConverterException;

/**
 * Provide support async protocols. {@link AsyncActionService} performs actions with annotation
 * {@linkplain AsyncAction @AsyncAction}. Every action is async message that contains message data as a field annotated
 * with {@linkplain Payload @Payload}.
 * <p>
 * Also {@linkplain AsyncActionService} has algorithm to synchronize outcoming and incoming messages.
 * Action could wait for response and store it to field with annotation {@linkplain PendingResponse @PendingResponse}.
 * Type of that field must be a class of incoming action. To link action with its response set class in the annotation implemented by
 * {@linkplain PendingResponseMatcher} where the condition for matching present.
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
    private final Converter converter;

    private final ConcurrentLinkedQueue<ActionHolder<ConnectAsyncAction>> connectActionQueue;
    private final ConcurrentLinkedQueue<ActionHolder<DisconnectAsyncAction>> disconnectActionQueue;
    private AsyncActionsRosterBase actionsRoster;
    private final AsyncActionSynchronizer synchronizer;
    private AsyncActionWrapperFactory actionWrapperFactory;
    private final List<Object> runningActions;


    public AsyncActionService(String url, AsyncClient client, Converter converter) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        if (client == null) {
            throw new IllegalArgumentException("client == null");
        }
        if (converter == null) {
            throw new IllegalArgumentException("converter == null");
        }
        this.url = url;
        this.client = client;
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
            if (wrapper.getResponseEvent() == null) {
                callback.onSuccess(holder);
            }
        } catch (CancelException ignored) {
        } finally {
            runningActions.remove(holder.action());
        }
    }

    private <A> AsyncActionWrapper getAsyncActionWrapper(ActionHolder<A> holder) {
        AsyncActionWrapper wrapper = actionWrapperFactory.make(holder);
        if (wrapper == null) {
            throw new JanetInternalException(ERROR_GENERATOR);
        }
        return wrapper;
    }

    @Override protected <A> void cancel(ActionHolder<A> holder) {
        AsyncActionWrapper wrapper = getAsyncActionWrapper(holder);
        runningActions.remove(holder.action());
        if (wrapper.getResponseEvent() != null) {
            synchronizer.remove(wrapper);
        }
    }

    private void sendAction(AsyncActionWrapper wrapper) throws AsyncServiceException, CancelException {
        String responseEvent = wrapper.getResponseEvent();
        if (responseEvent != null) {
            synchronizer.put(responseEvent, wrapper);
        }
        try {
            ActionBody actionBody = wrapper.getPayload(converter);
            byte[] content = actionBody.getContent();
            if (wrapper.isBytesPayload()) {
                client.send(wrapper.getEvent(), content);
            } else {
                client.send(wrapper.getEvent(), new String(content));
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
            disconnect();
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

    private void disconnect() throws AsyncServiceException {
        try {
            client.disconnect();
        } catch (Throwable t) {
            throw new AsyncServiceException(t);
        }
    }

    private void onMessageReceived(String event, BytesArrayBody body) {
        if (!actionsRoster.containsEvent(event)) {
            System.err.println(String.format("Received sync message %s is not defined by any action :(. The message contains body %s", event, body));
            return;
        }
        List<Class> actionClassList = actionsRoster.getActionClasses(event);
        for (Class actionClass : actionClassList) {
            ActionHolder holder = ActionHolder.create((createActionInstance(actionClass)));
            AsyncActionWrapper actionWrapper = getAsyncActionWrapper(holder);
            try {
                actionWrapper.fillPayload(body, converter);
            } catch (ConverterException e) {
                callback.onFail(holder, new AsyncServiceException(e));
            }
            if (synchronizer.contains(event)) {
                for (AsyncActionWrapper wrapper : synchronizer.sync(event, actionWrapper.action, new AsyncActionSynchronizer.Predicate() {
                    @Override public boolean call(AsyncActionWrapper wrapper, Object responseAction) {
                        try {
                            return wrapper.fillResponse(responseAction);
                        } catch (ConverterException e) {
                            callback.onFail(wrapper.holder, new AsyncServiceException(e));
                        }
                        return false;
                    }
                })) {
                    callback.onSuccess(wrapper.holder);
                }
            } else {
                callback.onSuccess(holder);
            }
        }
    }

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

        @Override public void onMessage(String event, String string) {
            BytesArrayBody body = null;
            if (string != null) {
                body = new StringBody(string);
            }
            onMessageReceived(event, body);
        }

        @Override public void onMessage(String event, byte[] bytes) {
            BytesArrayBody body = null;
            if (bytes != null) {
                body = new BytesArrayBody(null, bytes);
            }
            onMessageReceived(event, body);
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
