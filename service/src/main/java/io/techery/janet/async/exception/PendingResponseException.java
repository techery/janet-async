package io.techery.janet.async.exception;

import java.util.Locale;

public final class PendingResponseException extends Throwable {

    private PendingResponseException(String message) {
        super(message);
    }

    public static PendingResponseException forTimeout(long timeout) {
        return new PendingResponseException(String.format(Locale.getDefault(), "Timeout has expired (%d)", timeout));
    }

    public static PendingResponseException forLimit(int limit) {
        return new PendingResponseException(String.format(Locale.getDefault(), "Too much actions were sent. More then defined limit (%d)", limit));
    }
}
