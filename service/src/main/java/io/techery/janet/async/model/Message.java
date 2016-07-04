package io.techery.janet.async.model;

public final class Message {

    public enum Type {
        TEXT, BINARY
    }

    private final String event;
    private final Type type;
    private final Object data;

    private Message(Type type, String event, Object data) {
        this.type = type;
        this.event = event;
        this.data = data;
    }

    public static Message createTextMessage(String event, String data) {
        return new Message(Type.TEXT, event, data);
    }

    public static Message createBinaryMessage(String event, byte[] data) {
        return new Message(Type.BINARY, event, data);
    }

    public Type getType() {
        return type;
    }

    public Object getData() {
        return data;
    }

    public String getEvent() {
        return event;
    }

    public byte[] getDataAsBinary() {
        switch (type) {
            case BINARY:
                return (byte[]) data;
            case TEXT:
                return ((String) data).getBytes();
            default:
                throw new IllegalStateException("Message type is unknown");
        }
    }

    public String getDataAsText() {
        switch (type) {
            case BINARY:
                return new String((byte[]) data);
            case TEXT:
                return (String) data;
            default:
                throw new IllegalStateException("Message type is unknown");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message = (Message) o;

        if (event != null ? !event.equals(message.event) : message.event != null) return false;
        if (type != message.type) return false;
        return data != null ? data.equals(message.data) : message.data == null;

    }

    @Override
    public int hashCode() {
        int result = event != null ? event.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Message{" +
                "event='" + event + '\'' +
                ", type=" + type +
                ", data=" + data +
                '}';
    }
}
