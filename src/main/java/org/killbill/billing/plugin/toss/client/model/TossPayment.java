package org.killbill.billing.plugin.toss.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TossPayment {

    private final String version;
    private final String paymentKey;
    private final String type;
    private final String orderId;
    private final String orderName;
    private final String mId;
    private final String currency;
    private final String method;
    private final Long totalAmount;
    private final Long balanceAmount;
    private final String status;
    private final String requestedAt;
    private final String approvedAt;
    private final Boolean useEscrow;
    private final String transactionKey;
    private final String lastTransactionKey;
    private final Long suppliedAmount;
    private final Long vat;
    private final Long taxFreeAmount;
    private final List<TossCancel> cancels;
    private final TossCard card;
    private final TossReceipt receipt;
    private final TossEasyPay easyPay;
    private final TossError failure;

    @JsonCreator
    public TossPayment(
            @JsonProperty("version") String version,
            @JsonProperty("paymentKey") String paymentKey,
            @JsonProperty("type") String type,
            @JsonProperty("orderId") String orderId,
            @JsonProperty("orderName") String orderName,
            @JsonProperty("mId") String mId,
            @JsonProperty("currency") String currency,
            @JsonProperty("method") String method,
            @JsonProperty("totalAmount") Long totalAmount,
            @JsonProperty("balanceAmount") Long balanceAmount,
            @JsonProperty("status") String status,
            @JsonProperty("requestedAt") String requestedAt,
            @JsonProperty("approvedAt") String approvedAt,
            @JsonProperty("useEscrow") Boolean useEscrow,
            @JsonProperty("transactionKey") String transactionKey,
            @JsonProperty("lastTransactionKey") String lastTransactionKey,
            @JsonProperty("suppliedAmount") Long suppliedAmount,
            @JsonProperty("vat") Long vat,
            @JsonProperty("taxFreeAmount") Long taxFreeAmount,
            @JsonProperty("cancels") List<TossCancel> cancels,
            @JsonProperty("card") TossCard card,
            @JsonProperty("receipt") TossReceipt receipt,
            @JsonProperty("easyPay") TossEasyPay easyPay,
            @JsonProperty("failure") TossError failure) {
        this.version = version;
        this.paymentKey = paymentKey;
        this.type = type;
        this.orderId = orderId;
        this.orderName = orderName;
        this.mId = mId;
        this.currency = currency;
        this.method = method;
        this.totalAmount = totalAmount;
        this.balanceAmount = balanceAmount;
        this.status = status;
        this.requestedAt = requestedAt;
        this.approvedAt = approvedAt;
        this.useEscrow = useEscrow;
        this.transactionKey = transactionKey;
        this.lastTransactionKey = lastTransactionKey;
        this.suppliedAmount = suppliedAmount;
        this.vat = vat;
        this.taxFreeAmount = taxFreeAmount;
        this.cancels = cancels;
        this.card = card;
        this.receipt = receipt;
        this.easyPay = easyPay;
        this.failure = failure;
    }

    public String getVersion() { return version; }
    public String getPaymentKey() { return paymentKey; }
    public String getType() { return type; }
    public String getOrderId() { return orderId; }
    public String getOrderName() { return orderName; }
    public String getMId() { return mId; }
    public String getCurrency() { return currency; }
    public String getMethod() { return method; }
    public Long getTotalAmount() { return totalAmount; }
    public Long getBalanceAmount() { return balanceAmount; }
    public String getStatus() { return status; }
    public String getRequestedAt() { return requestedAt; }
    public String getApprovedAt() { return approvedAt; }
    public Boolean getUseEscrow() { return useEscrow; }
    public String getTransactionKey() { return transactionKey; }
    public String getLastTransactionKey() { return lastTransactionKey; }
    public Long getSuppliedAmount() { return suppliedAmount; }
    public Long getVat() { return vat; }
    public Long getTaxFreeAmount() { return taxFreeAmount; }
    public List<TossCancel> getCancels() { return cancels; }
    public TossCard getCard() { return card; }
    public TossReceipt getReceipt() { return receipt; }
    public TossEasyPay getEasyPay() { return easyPay; }
    public TossError getFailure() { return failure; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TossCancel {
        private final Long cancelAmount;
        private final String cancelReason;
        private final Long taxFreeAmount;
        private final Long taxExemptionAmount;
        private final Long refundableAmount;
        private final String canceledAt;
        private final String transactionKey;

        @JsonCreator
        public TossCancel(
                @JsonProperty("cancelAmount") Long cancelAmount,
                @JsonProperty("cancelReason") String cancelReason,
                @JsonProperty("taxFreeAmount") Long taxFreeAmount,
                @JsonProperty("taxExemptionAmount") Long taxExemptionAmount,
                @JsonProperty("refundableAmount") Long refundableAmount,
                @JsonProperty("canceledAt") String canceledAt,
                @JsonProperty("transactionKey") String transactionKey) {
            this.cancelAmount = cancelAmount;
            this.cancelReason = cancelReason;
            this.taxFreeAmount = taxFreeAmount;
            this.taxExemptionAmount = taxExemptionAmount;
            this.refundableAmount = refundableAmount;
            this.canceledAt = canceledAt;
            this.transactionKey = transactionKey;
        }

        public Long getCancelAmount() { return cancelAmount; }
        public String getCancelReason() { return cancelReason; }
        public Long getTaxFreeAmount() { return taxFreeAmount; }
        public Long getTaxExemptionAmount() { return taxExemptionAmount; }
        public Long getRefundableAmount() { return refundableAmount; }
        public String getCanceledAt() { return canceledAt; }
        public String getTransactionKey() { return transactionKey; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TossReceipt {
        private final String url;

        @JsonCreator
        public TossReceipt(@JsonProperty("url") String url) {
            this.url = url;
        }

        public String getUrl() { return url; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TossEasyPay {
        private final String provider;
        private final Long amount;
        private final Long discountAmount;

        @JsonCreator
        public TossEasyPay(
                @JsonProperty("provider") String provider,
                @JsonProperty("amount") Long amount,
                @JsonProperty("discountAmount") Long discountAmount) {
            this.provider = provider;
            this.amount = amount;
            this.discountAmount = discountAmount;
        }

        public String getProvider() { return provider; }
        public Long getAmount() { return amount; }
        public Long getDiscountAmount() { return discountAmount; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TossCard {
        private final String company;
        private final String number;
        private final Integer installmentPlanMonths;
        private final String approveNo;
        private final Boolean useCardPoint;
        private final String cardType;
        private final String ownerType;
        private final String acquireStatus;
        private final String receiptUrl;
        private final String issuerCode;
        private final String acquirerCode;
        private final Boolean isInterestFree;
        private final Long amount;

        @JsonCreator
        public TossCard(
                @JsonProperty("company") String company,
                @JsonProperty("number") String number,
                @JsonProperty("installmentPlanMonths") Integer installmentPlanMonths,
                @JsonProperty("approveNo") String approveNo,
                @JsonProperty("useCardPoint") Boolean useCardPoint,
                @JsonProperty("cardType") String cardType,
                @JsonProperty("ownerType") String ownerType,
                @JsonProperty("acquireStatus") String acquireStatus,
                @JsonProperty("receiptUrl") String receiptUrl,
                @JsonProperty("issuerCode") String issuerCode,
                @JsonProperty("acquirerCode") String acquirerCode,
                @JsonProperty("isInterestFree") Boolean isInterestFree,
                @JsonProperty("amount") Long amount) {
            this.company = company;
            this.number = number;
            this.installmentPlanMonths = installmentPlanMonths;
            this.approveNo = approveNo;
            this.useCardPoint = useCardPoint;
            this.cardType = cardType;
            this.ownerType = ownerType;
            this.acquireStatus = acquireStatus;
            this.receiptUrl = receiptUrl;
            this.issuerCode = issuerCode;
            this.acquirerCode = acquirerCode;
            this.isInterestFree = isInterestFree;
            this.amount = amount;
        }

        public String getCompany() { return company; }
        public String getNumber() { return number; }
        public Integer getInstallmentPlanMonths() { return installmentPlanMonths; }
        public String getApproveNo() { return approveNo; }
        public Boolean getUseCardPoint() { return useCardPoint; }
        public String getCardType() { return cardType; }
        public String getOwnerType() { return ownerType; }
        public String getAcquireStatus() { return acquireStatus; }
        public String getReceiptUrl() { return receiptUrl; }
        public String getIssuerCode() { return issuerCode; }
        public String getAcquirerCode() { return acquirerCode; }
        public Boolean getIsInterestFree() { return isInterestFree; }
        public Long getAmount() { return amount; }
    }
}
