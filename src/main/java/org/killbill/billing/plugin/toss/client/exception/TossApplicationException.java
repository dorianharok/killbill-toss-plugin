package org.killbill.billing.plugin.toss.client.exception;

import org.killbill.billing.plugin.toss.client.model.TossError;

public class TossApplicationException extends RuntimeException {

    private final TossError tossError;
    private final int statusCode;

    public TossApplicationException(TossError tossError, int statusCode) {
        super(tossError.getMessage());
        this.tossError = tossError;
        this.statusCode = statusCode;
    }

    public TossError getTossError() {
        return tossError;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
