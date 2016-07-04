package io.techery.janet;

import io.techery.janet.async.model.Message;

public abstract class AsyncClient {

    protected Callback callback;

    protected abstract boolean isConnected();

    protected abstract void connect(String url, boolean reconnectIfConnected) throws Throwable;

    protected abstract void disconnect() throws Throwable;

    protected abstract void send(Message message) throws Throwable;

    protected abstract void subscribe(String event);

    void setCallback(Callback callback) {
        this.callback = callback;
    }

    public interface Callback {
        void onConnect();

        void onDisconnect(String reason);

        void onConnectionError(Throwable throwable);

        void onError(Throwable throwable);

        void onMessage(Message message);
    }
}
