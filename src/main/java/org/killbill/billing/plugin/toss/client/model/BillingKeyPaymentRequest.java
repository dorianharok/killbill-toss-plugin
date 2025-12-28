package org.killbill.billing.plugin.toss.client.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for billing key payment execution.
 * Used with POST /v1/billing/{billingKey} API.
 *
 * @see <a href="https://docs.tosspayments.com/reference#%EB%B9%8C%EB%A7%81%ED%82%A4-%EA%B2%B0%EC%A0%9C-%EC%8A%B9%EC%9D%B8">Toss Payments Billing Key Payment API</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BillingKeyPaymentRequest {

    @JsonProperty("amount")
    private final Long amount;

    @JsonProperty("orderId")
    private final String orderId;

    @JsonProperty("orderName")
    private final String orderName;

    @JsonProperty("customerKey")
    private final String customerKey;

    @JsonProperty("customerEmail")
    private final String customerEmail;

    @JsonProperty("customerName")
    private final String customerName;

    public BillingKeyPaymentRequest(final Long amount,
                                    final String orderId,
                                    final String orderName,
                                    final String customerKey) {
        this(amount, orderId, orderName, customerKey, null, null);
    }

    public BillingKeyPaymentRequest(final Long amount,
                                    final String orderId,
                                    final String orderName,
                                    final String customerKey,
                                    final String customerEmail,
                                    final String customerName) {
        this.amount = amount;
        this.orderId = orderId;
        this.orderName = orderName;
        this.customerKey = customerKey;
        this.customerEmail = customerEmail;
        this.customerName = customerName;
    }

    public Long getAmount() {
        return amount;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getOrderName() {
        return orderName;
    }

    public String getCustomerKey() {
        return customerKey;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getCustomerName() {
        return customerName;
    }
}
