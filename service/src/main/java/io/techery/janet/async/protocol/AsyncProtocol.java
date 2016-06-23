package io.techery.janet.async.protocol;

import io.techery.janet.async.model.Message;

public final class AsyncProtocol {

    private final MessageRule<String> textMessageRule;
    private final MessageRule<byte[]> binaryMessageRule;
    private final ResponseMatcher responseMatcher;

    private AsyncProtocol(MessageRule<String> textMessageRule, MessageRule<byte[]> binaryMessageRule, ResponseMatcher responseMatcher) {
        this.textMessageRule = textMessageRule;
        this.binaryMessageRule = binaryMessageRule;
        this.responseMatcher = responseMatcher;
    }

    public MessageRule<String> textMessageRule() {
        return textMessageRule;
    }

    public MessageRule<byte[]> binaryMessageRule() {
        return binaryMessageRule;
    }

    public ResponseMatcher responseMatcher() {
        return responseMatcher;
    }

    public final static class Builder {
        private MessageRule<String> textMessageRule = new SimpleTextMessageRule();
        private MessageRule<byte[]> binaryMessageRule = new SimpleBinaryMessageRule();
        private ResponseMatcher responseMatcher;

        public Builder setTextMessageRule(MessageRule<String> converter) {
            if (converter == null) {
                throw new IllegalArgumentException("converter == null");
            }
            this.textMessageRule = converter;
            return this;
        }

        public Builder setBinaryMessageRule(MessageRule<byte[]> converter) {
            if (converter == null) {
                throw new IllegalArgumentException("converter == null");
            }
            this.binaryMessageRule = converter;
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
            return new AsyncProtocol(textMessageRule, binaryMessageRule, responseMatcher);
        }
    }

    private static class SimpleTextMessageRule implements MessageRule<String> {

        @Override public String handleMessage(Message message) {
            return message.getDataAsText();
        }

        @Override public Message createMessage(String event, String payload) {
            return Message.createTextMessage(event, payload);
        }
    }

    private static class SimpleBinaryMessageRule implements MessageRule<byte[]> {
        @Override public byte[] handleMessage(Message message) {
            return message.getDataAsBinary();
        }

        @Override public Message createMessage(String event, byte[] payload) {
            return Message.createBinaryMessage(event, payload);
        }
    }
}
