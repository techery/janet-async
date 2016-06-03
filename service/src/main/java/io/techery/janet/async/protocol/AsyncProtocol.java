package io.techery.janet.async.protocol;

import io.techery.janet.async.model.Message;

public final class AsyncProtocol {

    private final PayloadConverter<String> textPayloadConverter;
    private final PayloadConverter<byte[]> binaryPayloadConverter;
    private final ResponseMatcher responseMatcher;

    private AsyncProtocol(PayloadConverter<String> textPayloadConverter, PayloadConverter<byte[]> binaryPayloadConverter, ResponseMatcher responseMatcher) {
        this.textPayloadConverter = textPayloadConverter;
        this.binaryPayloadConverter = binaryPayloadConverter;
        this.responseMatcher = responseMatcher;
    }

    public PayloadConverter<String> textPayloadConverter() {
        return textPayloadConverter;
    }

    public PayloadConverter<byte[]> binaryPayloadConverter() {
        return binaryPayloadConverter;
    }

    public ResponseMatcher responseMatcher() {
        return responseMatcher;
    }

    public final static class Builder {
        private PayloadConverter<String> textPayloadConverter = new SimpleTextPayloadConverter();
        private PayloadConverter<byte[]> binaryPayloadConverter = new SimpleBinaryPayloadConverter();
        private ResponseMatcher responseMatcher;

        public Builder setTextPayloadConverter(PayloadConverter<String> converter) {
            if (converter == null) {
                throw new IllegalArgumentException("converter == null");
            }
            this.textPayloadConverter = converter;
            return this;
        }

        public Builder setBinaryPayloadConverter(PayloadConverter<byte[]> converter) {
            if (converter == null) {
                throw new IllegalArgumentException("converter == null");
            }
            this.binaryPayloadConverter = converter;
            return this;
        }

        public Builder setResponseMatcher(ResponseMatcher responseMatcher) {
            if (responseMatcher == null) {
                throw new IllegalArgumentException("responseMatcher == null");
            }
            this.responseMatcher = responseMatcher;
            return this;
        }

        public AsyncProtocol build() {
            return new AsyncProtocol(textPayloadConverter, binaryPayloadConverter, responseMatcher);
        }
    }

    private static class SimpleTextPayloadConverter implements PayloadConverter<String> {

        @Override public String extractPayload(Message message) {
            return message.getDataAsText();
        }

        @Override public Message createMessage(String event, String payload) {
            return Message.createTextMessage(event, payload);
        }
    }

    private static class SimpleBinaryPayloadConverter implements PayloadConverter<byte[]> {
        @Override public byte[] extractPayload(Message message) {
            return message.getDataAsBinary();
        }

        @Override public Message createMessage(String event, byte[] payload) {
            return Message.createBinaryMessage(event, payload);
        }
    }
}
