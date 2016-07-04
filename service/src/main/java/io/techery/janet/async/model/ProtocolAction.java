package io.techery.janet.async.model;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.techery.janet.body.ActionBody;
import io.techery.janet.body.BytesArrayBody;
import io.techery.janet.body.StringBody;

public final class ProtocolAction {

    private final String event;
    private final ActionBody payload;
    private final boolean isBinaryPayload;
    private final Metadata metadata = new Metadata();

    private ProtocolAction(String event, ActionBody payload, boolean isBinaryPayload) {
        this.event = event;
        this.payload = payload;
        this.isBinaryPayload = isBinaryPayload;
    }

    public String getEvent() {
        return event;
    }

    public ActionBody getPayload() {
        return payload;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    public ProtocolAction metadata(String key, Object value) {
        metadata.put(key, value);
        return this;
    }

    public String getPayloadAsString() {
        if (payload != null) {
            return payload.toString();
        }
        return null;
    }

    public byte[] getPayloadAsBinary() {
        if (payload != null) {
            try {
                return payload.getContent();
            } catch (IOException ignored) {}
        }
        return null;
    }

    public boolean isBinaryPayload() {
        return isBinaryPayload;
    }

    public static PayloadMethod of(String event) {
        return new PayloadMethod(event);
    }

    public static PayloadMethod of(Message message) {
        return new PayloadMethod(message.getEvent());
    }

    public static ProtocolAction from(Message message) {
        return new PayloadMethod(message.getEvent()).payload(message.getDataAsBinary());
    }

    public static class PayloadMethod {

        private final String event;

        private PayloadMethod(String event) {this.event = event;}

        public ProtocolAction payload(String payload) {
            return payload(new StringBody(payload), false);
        }

        public ProtocolAction payload(byte[] payload) {
            return payload(new BytesArrayBody(null, payload), true);
        }

        public ProtocolAction payload(ActionBody actionBody, boolean isBinary) {
            return new ProtocolAction(event, actionBody, isBinary);
        }
    }

    public static class Metadata {

        private final Map<String, Object> data = new ConcurrentHashMap<String, Object>();

        public void put(String key, Object value) {
            data.put(key, value);
        }

        public Object get(String key) {
            return data.get(key);
        }
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProtocolAction that = (ProtocolAction) o;

        if (isBinaryPayload != that.isBinaryPayload) return false;
        if (event != null ? !event.equals(that.event) : that.event != null) return false;
        return payload != null ? payload.equals(that.payload)
                : that.payload == null && metadata.equals(that.metadata);

    }

    @Override public int hashCode() {
        int result = event != null ? event.hashCode() : 0;
        result = 31 * result + (payload != null ? payload.hashCode() : 0);
        result = 31 * result + (isBinaryPayload ? 1 : 0);
        result = 31 * result + (metadata.hashCode());
        return result;
    }

    @Override public String toString() {
        return "ProtocolAction{" +
                "event='" + event + '\'' +
                ", payload=" + payload +
                ", isBinaryPayload=" + isBinaryPayload +
                ", metadata=" + metadata +
                '}';
    }
}
