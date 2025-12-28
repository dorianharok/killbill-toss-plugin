package org.killbill.billing.plugin.toss.client.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentCancelRequest {

    @JsonProperty("cancelReason")
    private final String cancelReason;

    @JsonProperty("cancelAmount")
    private final Long cancelAmount;

    @JsonProperty("refundReceiveAccount")
    private final RefundReceiveAccount refundReceiveAccount;

    @JsonProperty("taxFreeAmount")
    private final Long taxFreeAmount;

    @JsonProperty("currency")
    private final String currency;

    public PaymentCancelRequest(String cancelReason, Long cancelAmount) {
        this(cancelReason, cancelAmount, null, null, null);
    }

    public PaymentCancelRequest(String cancelReason, Long cancelAmount, RefundReceiveAccount refundReceiveAccount, Long taxFreeAmount, String currency) {
        this.cancelReason = cancelReason;
        this.cancelAmount = cancelAmount;
        this.refundReceiveAccount = refundReceiveAccount;
        this.taxFreeAmount = taxFreeAmount;
        this.currency = currency;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public Long getCancelAmount() {
        return cancelAmount;
    }

    public RefundReceiveAccount getRefundReceiveAccount() {
        return refundReceiveAccount;
    }

    public Long getTaxFreeAmount() {
        return taxFreeAmount;
    }

    public String getCurrency() {
        return currency;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RefundReceiveAccount {
        @JsonProperty("bank")
        private final String bank;

        @JsonProperty("accountNumber")
        private final String accountNumber;

        @JsonProperty("holderName")
        private final String holderName;

        public RefundReceiveAccount(String bank, String accountNumber, String holderName) {
            this.bank = bank;
            this.accountNumber = accountNumber;
            this.holderName = holderName;
        }

        public String getBank() {
            return bank;
        }

        public String getAccountNumber() {
            return accountNumber;
        }

        public String getHolderName() {
            return holderName;
        }
    }
}
