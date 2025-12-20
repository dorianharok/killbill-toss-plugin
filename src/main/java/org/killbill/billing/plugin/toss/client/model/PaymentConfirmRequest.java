package org.killbill.billing.plugin.toss.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PaymentConfirmRequest {

    @JsonProperty("paymentKey")
    private final String paymentKey;

    @JsonProperty("orderId")
    private final String orderId;

    @JsonProperty("amount")
    private final Long amount;

    public PaymentConfirmRequest(String paymentKey, String orderId, Long amount) {
        this.paymentKey = paymentKey;
        this.orderId = orderId;
        this.amount = amount;
    }

    public String getPaymentKey() {
        return paymentKey;
    }

    public String getOrderId() {
        return orderId;
    }

    public Long getAmount() {
        return amount;
    }
}
