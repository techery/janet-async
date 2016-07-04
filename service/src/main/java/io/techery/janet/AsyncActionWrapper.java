package io.techery.janet;

import java.lang.reflect.Type;
import java.util.concurrent.ScheduledFuture;

import io.techery.janet.async.model.ProtocolAction;

public abstract class AsyncActionWrapper<A> {

    final ActionHolder<A> holder;
    protected final A action;
    protected ProtocolAction protocolAction;
    private ScheduledFuture scheduledFuture;

    protected AsyncActionWrapper(ActionHolder<A> holder) {
        this.holder = holder;
        this.action = holder.action();
    }

    protected abstract String getEvent();
    protected abstract boolean isBytesPayload();
    protected abstract Type getPayloadFieldType();
    protected abstract Object getPayload();
    protected abstract void setPayload(Object payload);
    protected abstract Type getResponseFieldType();
    protected abstract void setResponse(Object response);

    protected long getResponseTimeout() {
        return AsyncActionSynchronizer.PENDING_TIMEOUT;
    }

    public boolean awaitsResponse() {
        return getResponseFieldType() != null;
    }

    public void cancelExpireFuture() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
        }
    }

    public void setExpireFuture(ScheduledFuture scheduledFuture) {
        this.scheduledFuture = scheduledFuture;
    }

    public void setProtocolAction(ProtocolAction protocolAction) {
        this.protocolAction = protocolAction;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AsyncActionWrapper<?> wrapper = (AsyncActionWrapper<?>) o;

        if (action == wrapper.action) return true;

        return action != null ? action.equals(wrapper.action) : wrapper.action == null;

    }

    @Override public int hashCode() {
        return action != null ? action.hashCode() : 0;
    }
}
