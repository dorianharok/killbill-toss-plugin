package org.killbill.billing.plugin.toss.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BillingKeyRequest {

    @JsonProperty("customerKey")
    private final String customerKey;

    @JsonProperty("authKey")
    private final String authKey;

    public BillingKeyRequest(String customerKey, String authKey) {
        this.customerKey = customerKey;
        this.authKey = authKey;
    }

    public String getAuthKey() {
        return authKey;
    }

    public String getCustomerKey() {
        return customerKey;
    }
}
