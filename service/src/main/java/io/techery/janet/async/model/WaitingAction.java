package io.techery.janet.async.model;

import java.lang.reflect.Type;

public class WaitingAction {

    private final Message message;
    private final Object parsedPayload;
    private final Type payloadFieldType;
    private boolean isBytePayload;

    public WaitingAction(Message message, Object parsedPayload, Type payloadFieldType, boolean isBytePayload) {
        this.message = message;
        this.parsedPayload = parsedPayload;
        this.payloadFieldType = payloadFieldType;
        this.isBytePayload = isBytePayload;
    }

    public Message getMessage() {
        return message;
    }

    public Object getParsedPayload() {
        return parsedPayload;
    }

    public Type getPayloadFieldType() {
        return payloadFieldType;
    }

    public boolean isBinaryPayload() {
        return isBytePayload;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WaitingAction action = (WaitingAction) o;

        if (isBytePayload != action.isBytePayload) return false;
        if (message != null ? !message.equals(action.message) : action.message != null) return false;
        if (parsedPayload != null ? !parsedPayload.equals(action.parsedPayload) : action.parsedPayload != null)
            return false;
        return payloadFieldType != null ? payloadFieldType.equals(action.payloadFieldType) : action.payloadFieldType == null;

    }

    @Override public int hashCode() {
        int result = message != null ? message.hashCode() : 0;
        result = 31 * result + (parsedPayload != null ? parsedPayload.hashCode() : 0);
        result = 31 * result + (payloadFieldType != null ? payloadFieldType.hashCode() : 0);
        result = 31 * result + (isBytePayload ? 1 : 0);
        return result;
    }

    @Override public String toString() {
        return "WaitingAction{" +
                "message=" + message +
                ", parsedPayload=" + parsedPayload +
                ", payloadFieldType=" + payloadFieldType +
                '}';
    }
}
