package org.killbill.billing.plugin.toss.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TossBilling {

    private final String mId;
    private final String customerKey;
    private final String authenticatedAt;
    private final String method;
    private final String billingKey;
    private final Card card;
    private final List<Transfer> transfers;
    private final String cardCompany;
    private final String cardNumber;

    @JsonCreator
    public TossBilling(
            @JsonProperty("mId") String mId,
            @JsonProperty("customerKey") String customerKey,
            @JsonProperty("authenticatedAt") String authenticatedAt,
            @JsonProperty("method") String method,
            @JsonProperty("billingKey") String billingKey,
            @JsonProperty("card") Card card,
            @JsonProperty("transfers") List<Transfer> transfers,
            @JsonProperty("cardCompany") String cardCompany,
            @JsonProperty("cardNumber") String cardNumber) {
        this.mId = mId;
        this.customerKey = customerKey;
        this.authenticatedAt = authenticatedAt;
        this.method = method;
        this.billingKey = billingKey;
        this.card = card;
        this.transfers = transfers;
        this.cardCompany = cardCompany;
        this.cardNumber = cardNumber;
    }

    public String getMId() {
        return mId;
    }

    public String getCustomerKey() {
        return customerKey;
    }

    public String getAuthenticatedAt() {
        return authenticatedAt;
    }

    public String getMethod() {
        return method;
    }

    public String getBillingKey() {
        return billingKey;
    }

    public Card getCard() {
        return card;
    }

    public List<Transfer> getTransfers() {
        return transfers;
    }

    public String getCardCompany() {
        return cardCompany;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Card {
        private final String issuerCode;
        private final String acquirerCode;
        private final String number;
        private final String cardType;
        private final String ownerType;

        @JsonCreator
        public Card(
                @JsonProperty("issuerCode") String issuerCode,
                @JsonProperty("acquirerCode") String acquirerCode,
                @JsonProperty("number") String number,
                @JsonProperty("cardType") String cardType,
                @JsonProperty("ownerType") String ownerType) {
            this.issuerCode = issuerCode;
            this.acquirerCode = acquirerCode;
            this.number = number;
            this.cardType = cardType;
            this.ownerType = ownerType;
        }

        public String getIssuerCode() { return issuerCode; }
        public String getAcquirerCode() { return acquirerCode; }
        public String getNumber() { return number; }
        public String getCardType() { return cardType; }
        public String getOwnerType() { return ownerType; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Transfer {
        private final String bankName;
        private final String bankAccountNumber;

        @JsonCreator
        public Transfer(
                @JsonProperty("bankName") String bankName,
                @JsonProperty("bankAccountNumber") String bankAccountNumber) {
            this.bankName = bankName;
            this.bankAccountNumber = bankAccountNumber;
        }

        public String getBankName() { return bankName; }
        public String getBankAccountNumber() { return bankAccountNumber; }
    }
}
