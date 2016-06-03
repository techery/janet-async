package io.techery.janet.async.model;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import io.techery.janet.body.BytesArrayBody;
import io.techery.janet.converter.Converter;

public final class IncomingMessage {

    private Map<Type, Object> parsedPayloads = new HashMap<Type, Object>();
    private boolean destroyed;
    private final Message message;
    private final BytesArrayBody payloadBody;
    private final Converter converter;

    public IncomingMessage(Message message, BytesArrayBody payloadBody, Converter converter) {
        this.message = message;
        this.payloadBody = payloadBody;
        this.converter = converter;
    }

    public Object getPayloadAs(Type type) {
        if (destroyed)
            return null;

        Object payload = parsedPayloads.get(type);
        if (payload == null) {
            payload = converter.fromBody(payloadBody, type);
            parsedPayloads.put(type, payload);
        }
        return payload;
    }

    @SuppressWarnings("unchecked")
    public <T> T getPayloadAs(Class<T> type) {
        return (T) getPayloadAs((Type) type);
    }

    public void destroy() {
        parsedPayloads.clear();
        destroyed = true;
    }

    public Message getMessage() {
        return message;
    }

    public BytesArrayBody getPayloadBody() {
        return payloadBody;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IncomingMessage that = (IncomingMessage) o;

        if (destroyed != that.destroyed) return false;
        if (message != null ? !message.equals(that.message) : that.message != null) return false;
        return payloadBody != null ? payloadBody.equals(that.payloadBody) : that.payloadBody == null;

    }

    @Override public int hashCode() {
        int result = (destroyed ? 1 : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (payloadBody != null ? payloadBody.hashCode() : 0);
        return result;
    }

    @Override public String toString() {
        return "IncomingMessage{" +
                "parsedPayloads=" + parsedPayloads +
                ", destroyed=" + destroyed +
                ", message=" + message +
                ", payloadBody=" + payloadBody +
                '}';
    }
}
