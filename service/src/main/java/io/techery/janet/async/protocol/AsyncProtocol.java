package io.techery.janet.async.protocol;

import io.techery.janet.async.model.Message;
import io.techery.janet.async.model.ProtocolAction;

public final class AsyncProtocol {

    private final MessageRule messageRule;
    private final ResponseMatcher responseMatcher;

    private AsyncProtocol(MessageRule messageRule, ResponseMatcher responseMatcher) {
        this.messageRule = messageRule;
        this.responseMatcher = responseMatcher;
    }

    public MessageRule messageRule() {
        return messageRule;
    }

    public ResponseMatcher responseMatcher() {
        return responseMatcher;
    }

    public final static class Builder {
        private MessageRule messageRule = new SimpleMessageRule();
        private ResponseMatcher responseMatcher;

        public Builder setMessageRule(MessageRule messageRule) {
            if (messageRule == null) {
                throw new IllegalArgumentException("converter == null");
            }
            this.messageRule = messageRule;
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
            return new AsyncProtocol(messageRule, responseMatcher);
        }
    }

    private static class SimpleMessageRule implements MessageRule {

        @Override public ProtocolAction handleMessage(Message message) {
            return ProtocolAction.from(message);
        }

        @Override public Message createMessage(ProtocolAction protocolAction) {
            if (protocolAction.isBinaryPayload()) {
                return Message.createBinaryMessage(protocolAction.getEvent(), protocolAction.getPayloadAsBinary());
            } else {
                return Message.createTextMessage(protocolAction.getEvent(), protocolAction.getPayloadAsString());
            }
        }
    }

}
