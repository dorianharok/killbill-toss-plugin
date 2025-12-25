package org.killbill.billing.plugin.toss.api;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.joda.time.DateTime;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.payment.api.PaymentMethodPlugin;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.GatewayNotification;
import org.killbill.billing.payment.plugin.api.HostedPaymentPageFormDescriptor;
import org.killbill.billing.payment.plugin.api.PaymentMethodInfoPlugin;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.billing.plugin.api.payment.PluginPaymentPluginApi;
import org.killbill.billing.plugin.toss.client.TossClient;
import org.killbill.billing.plugin.toss.client.exception.TossApplicationException;
import org.killbill.billing.plugin.toss.client.model.PaymentCancelRequest;
import org.killbill.billing.plugin.toss.client.model.PaymentConfirmRequest;
import org.killbill.billing.plugin.toss.client.model.TossPayment;
import org.killbill.billing.plugin.toss.core.TossConfigProperties;
import org.killbill.billing.plugin.toss.core.TossConfigurationHandler;
import org.killbill.billing.plugin.toss.dao.TossDao;
import org.killbill.billing.plugin.toss.dao.gen.tables.TossPaymentMethods;
import org.killbill.billing.plugin.toss.dao.gen.tables.TossResponses;
import org.killbill.billing.plugin.toss.dao.gen.tables.records.TossPaymentMethodsRecord;
import org.killbill.billing.plugin.toss.dao.gen.tables.records.TossResponsesRecord;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.billing.util.entity.Pagination;
import org.killbill.clock.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TossPaymentPluginApi extends PluginPaymentPluginApi<TossResponsesRecord, TossResponses, TossPaymentMethodsRecord, TossPaymentMethods> {

    private static final Logger logger = LoggerFactory.getLogger(TossPaymentPluginApi.class);

    private final TossDao dao;
    private final TossConfigurationHandler configurationHandler;
    private final TossClient tossClient;

    public TossPaymentPluginApi(final OSGIKillbillAPI killbillAPI,
                                final OSGIConfigPropertiesService configProperties,
                                final Clock clock,
                                final TossDao dao,
                                final TossConfigurationHandler configurationHandler,
                                final TossClient tossClient) {
        super(killbillAPI, configProperties, clock, dao);
        this.dao = dao;
        this.configurationHandler = configurationHandler;
        this.tossClient = tossClient;
    }

    /**
     * Get the Toss configuration for a specific tenant.
     *
     * @param context tenant context
     * @return TossConfigProperties for the given tenant
     */
    protected TossConfigProperties getConfigForTenant(final TenantContext context) {
        return configurationHandler.getConfigurable(context.getTenantId());
    }

    @Override
    public PaymentTransactionInfoPlugin authorizePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        logger.info("authorizePayment called - operation not supported for Korean PG");
        return TossPaymentTransactionInfoPlugin.unimplementedAPI(kbPaymentId, kbTransactionId, TransactionType.AUTHORIZE, "authorizePayment");
    }

    @Override
    public PaymentTransactionInfoPlugin capturePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        logger.info("capturePayment called - operation not supported for Korean PG");
        return TossPaymentTransactionInfoPlugin.unimplementedAPI(kbPaymentId, kbTransactionId, TransactionType.CAPTURE, "capturePayment");
    }

    @Override
    public PaymentTransactionInfoPlugin purchasePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        logger.info("purchasePayment called: kbPaymentId={}, amount={}, currency={}", kbPaymentId, amount, currency);

        String paymentKey = null;
        try {
            // Extract Toss parameters from plugin properties
            paymentKey = PluginProperties.findPluginPropertyValue("paymentKey", properties);
            if (paymentKey == null) {
                throw new PaymentPluginApiException("ERROR", "Missing required property: paymentKey");
            }

            final String orderId = PluginProperties.getValue("orderId", kbPaymentId.toString(), properties);

            final Long tossAmount = amount.longValue(); // Toss uses integer won (KRW), not cents

            // Get tenant configuration
            final TossConfigProperties config = getConfigForTenant(context);
            final String secretKey = config.getSecretKey();

            // Call Toss API
            final PaymentConfirmRequest request = new PaymentConfirmRequest(paymentKey, orderId, tossAmount);
            final String idempotencyKey = kbTransactionId.toString();
            final TossPayment tossPayment = tossClient.confirmPayment(secretKey, request, idempotencyKey);

            // Build success response
            final PaymentTransactionInfoPlugin response = buildPaymentTransactionInfo(
                    kbPaymentId,
                    kbTransactionId,
                    TransactionType.PURCHASE,
                    amount,
                    currency,
                    tossPayment
            );

            // Save to database
            try {
                dao.addResponse(kbAccountId, kbPaymentId, kbTransactionId, TransactionType.PURCHASE, amount, currency, paymentKey, tossPayment, null, clock.getUTCNow(), context.getTenantId());
            } catch (final Exception dbError) {
                logger.error("Failed to save response to database", dbError);
                throw new PaymentPluginApiException("Failed to save payment response to database: " + dbError.getMessage(), dbError);
            }

            logger.info("purchasePayment succeeded: paymentKey={}, status={}", paymentKey, tossPayment.getStatus());
            return response;

        } catch (final TossApplicationException e) {
            logger.error("Toss API error: code={}, message={}", e.getTossError().getCode(), e.getTossError().getMessage());
            final PaymentTransactionInfoPlugin errorResponse = buildErrorResponse(kbPaymentId, kbTransactionId, TransactionType.PURCHASE, amount, currency, e);

            // Save error to database
            try {
                dao.addResponse(kbAccountId, kbPaymentId, kbTransactionId, TransactionType.PURCHASE, amount, currency, paymentKey, null, e, clock.getUTCNow(), context.getTenantId());
            } catch (final Exception dbError) {
                logger.error("Failed to save error response to database", dbError);
                // Don't throw here - we already have the payment error to return
            }

            return errorResponse;

        } catch (final IOException | InterruptedException e) {
            logger.error("Network error during payment confirmation", e);
            final PaymentTransactionInfoPlugin pendingResponse = buildPendingResponse(kbPaymentId, kbTransactionId, TransactionType.PURCHASE, amount, currency, paymentKey, e);

            try {
                dao.addResponse(kbAccountId, kbPaymentId, kbTransactionId, TransactionType.PURCHASE, amount, currency, paymentKey, null, null, clock.getUTCNow(), context.getTenantId());
            } catch (final Exception dbError) {
                logger.error("Failed to save pending response to database", dbError);
            }

            return pendingResponse;
        }
    }

    @Override
    public PaymentTransactionInfoPlugin voidPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        logger.info("voidPayment called - operation not supported for Korean PG");
        return TossPaymentTransactionInfoPlugin.unimplementedAPI(kbPaymentId, kbTransactionId, TransactionType.VOID, "voidPayment");
    }

    @Override
    public PaymentTransactionInfoPlugin creditPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        logger.info("creditPayment called - operation not supported for Korean PG");
        return TossPaymentTransactionInfoPlugin.unimplementedAPI(kbPaymentId, kbTransactionId, TransactionType.CREDIT, "creditPayment");
    }

    @Override
    public PaymentTransactionInfoPlugin refundPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        logger.info("refundPayment called: kbPaymentId={}, amount={}, currency={}", kbPaymentId, amount, currency);

        final TossResponsesRecord previousRecord;
        try {
            previousRecord = dao.getResponseByPaymentId(kbPaymentId, context.getTenantId());
        } catch (final java.sql.SQLException e) {
            logger.error("Database error while retrieving original payment for refund", e);
            throw new PaymentPluginApiException("DATABASE_ERROR", "Failed to retrieve original payment: " + e.getMessage());
        }

        validateRefund(previousRecord, amount, kbPaymentId);

        final String paymentKey = previousRecord.getPaymentKey();

        try {
            final Long cancelAmount = (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) ? amount.longValue() : null;
            final String cancelReason = PluginProperties.getValue("cancelReason", "고객 요청에 의한 환불", properties);

            final TossConfigProperties config = getConfigForTenant(context);
            final String secretKey = config.getSecretKey();

            final PaymentCancelRequest request = new PaymentCancelRequest(cancelReason, cancelAmount);
            final String idempotencyKey = kbTransactionId.toString();
            final TossPayment tossPayment = tossClient.cancelPayment(secretKey, paymentKey, request, idempotencyKey);

            final PaymentTransactionInfoPlugin response = buildRefundTransactionInfo(
                    kbPaymentId,
                    kbTransactionId,
                    amount != null ? amount : previousRecord.getAmount(),
                    currency,
                    tossPayment
            );

            try {
                dao.addResponse(kbAccountId, kbPaymentId, kbTransactionId, TransactionType.REFUND, amount, currency, paymentKey, tossPayment, null, clock.getUTCNow(), context.getTenantId());
            } catch (final java.sql.SQLException dbError) {
                logger.error("Failed to save refund response to database", dbError);
            }

            logger.info("refundPayment succeeded: paymentKey={}, cancelAmount={}", paymentKey, cancelAmount);
            return response;

        } catch (final TossApplicationException e) {
            logger.error("Toss API error during refund: code={}, message={}", e.getTossError().getCode(), e.getTossError().getMessage());
            final PaymentTransactionInfoPlugin errorResponse = buildErrorResponse(kbPaymentId, kbTransactionId, TransactionType.REFUND, amount, currency, e);

            try {
                dao.addResponse(kbAccountId, kbPaymentId, kbTransactionId, TransactionType.REFUND, amount, currency, paymentKey, null, e, clock.getUTCNow(), context.getTenantId());
            } catch (final java.sql.SQLException dbError) {
                logger.error("Failed to save refund error response to database", dbError);
            }

            return errorResponse;

        } catch (final IOException | InterruptedException e) {
            logger.error("Network error during refund", e);
            final PaymentTransactionInfoPlugin pendingResponse = buildPendingResponse(kbPaymentId, kbTransactionId, TransactionType.REFUND, amount, currency, paymentKey, e);

            try {
                dao.addResponse(kbAccountId, kbPaymentId, kbTransactionId, TransactionType.REFUND, amount, currency, paymentKey, null, null, clock.getUTCNow(), context.getTenantId());
            } catch (final java.sql.SQLException dbError) {
                logger.error("Failed to save refund pending response to database", dbError);
            }

            return pendingResponse;
        }
    }

    private void validateRefund(final TossResponsesRecord previousRecord,
                                final BigDecimal amount,
                                final UUID kbPaymentId) throws PaymentPluginApiException {
        if (previousRecord == null || previousRecord.getPaymentKey() == null) {
            logger.error("Cannot find original payment for refund: kbPaymentId={}", kbPaymentId);
            throw new PaymentPluginApiException("REFUND_ERROR", "Original payment not found for kbPaymentId=" + kbPaymentId);
        }

        if (!"DONE".equals(previousRecord.getTossPaymentStatus())) {
            logger.error("Cannot refund payment with status {}: kbPaymentId={}", previousRecord.getTossPaymentStatus(), kbPaymentId);
            throw new PaymentPluginApiException("REFUND_ERROR", "Original payment is not in DONE status, current status=" + previousRecord.getTossPaymentStatus());
        }

        if (amount != null) {
            if (amount.compareTo(BigDecimal.ZERO) == 0) {
                logger.error("Refund amount cannot be zero: kbPaymentId={}", kbPaymentId);
                throw new PaymentPluginApiException("REFUND_ERROR", "Refund amount cannot be zero");
            }

            if (previousRecord.getAmount() != null && amount.compareTo(previousRecord.getAmount()) > 0) {
                logger.error("Refund amount {} exceeds original payment amount {}: kbPaymentId={}",
                        amount, previousRecord.getAmount(), kbPaymentId);
                throw new PaymentPluginApiException("REFUND_ERROR", "Refund amount " + amount + " exceeds original payment amount " + previousRecord.getAmount());
            }
        }
    }

    @Override
    public List<PaymentTransactionInfoPlugin> getPaymentInfo(final UUID kbAccountId, final UUID kbPaymentId, final Iterable<PluginProperty> properties, final TenantContext context) throws PaymentPluginApiException {
        logger.info("getPaymentInfo called: kbPaymentId={}", kbPaymentId);

        // 1. Get existing transactions to find the correct kbTransactionId
        final List<PaymentTransactionInfoPlugin> transactions = super.getPaymentInfo(kbAccountId, kbPaymentId, properties, context);
        if (transactions.isEmpty()) {
            return transactions;
        }

        // 2. Filter for PENDING or UNDEFINED transactions that need update
        // Simple implementation: try to update the latest transaction if it's not final
        final PaymentTransactionInfoPlugin lastTransaction = transactions.get(transactions.size() - 1);
        if (lastTransaction.getStatus() == PaymentPluginStatus.PROCESSED || lastTransaction.getStatus() == PaymentPluginStatus.CANCELED || lastTransaction.getStatus() == PaymentPluginStatus.ERROR) {
            // Already final, return as is
            return transactions;
        }

        final UUID kbTransactionId = lastTransaction.getKbTransactionPaymentId();
        String paymentKey = PluginProperties.findPluginPropertyValue("paymentKey", properties);
        
        // If not in properties, try to get from the transaction info
        if (paymentKey == null) {
            paymentKey = lastTransaction.getFirstPaymentReferenceId();
        }

        // If still null, try database
        if (paymentKey == null) {
             try {
                final TossResponsesRecord dbRecord = dao.getResponseByPaymentId(kbPaymentId, context.getTenantId());
                if (dbRecord != null) {
                    paymentKey = dbRecord.getPaymentKey();
                }
            } catch (final Exception e) {
                logger.warn("Failed to retrieve paymentKey from DB", e);
            }
        }

        if (paymentKey == null) {
            logger.warn("Could not find paymentKey for kbPaymentId={}, cannot sync with Toss", kbPaymentId);
            return transactions;
        }

        try {
            // Get tenant configuration
            final TossConfigProperties config = getConfigForTenant(context);
            final String secretKey = config.getSecretKey();

            // Call Toss API to get latest payment status
            final TossPayment tossPayment = tossClient.getPayment(secretKey, paymentKey);

            // Build response with CORRECT kbTransactionId
            final PaymentTransactionInfoPlugin response = buildPaymentTransactionInfo(
                    kbPaymentId,
                    kbTransactionId, // Use existing transaction ID
                    lastTransaction.getTransactionType(),
                    BigDecimal.valueOf(tossPayment.getTotalAmount()),
                    Currency.valueOf(tossPayment.getCurrency()),
                    tossPayment
            );

            // Update database with latest status from Toss
            try {
                dao.addResponse(kbAccountId, kbPaymentId, kbTransactionId, lastTransaction.getTransactionType(),
                               BigDecimal.valueOf(tossPayment.getTotalAmount()),
                               Currency.valueOf(tossPayment.getCurrency()), paymentKey, tossPayment, null, clock.getUTCNow(), context.getTenantId());
            } catch (final Exception dbError) {
                logger.error("Failed to update payment status in database", dbError);
            }

            logger.info("getPaymentInfo synced: paymentKey={}, status={}", paymentKey, tossPayment.getStatus());
            
            // Return mutable list with updated transaction
            final List<PaymentTransactionInfoPlugin> updatedTransactions = new java.util.ArrayList<>(transactions);
            updatedTransactions.set(transactions.size() - 1, response);
            return updatedTransactions;

        } catch (final TossApplicationException e) {
            logger.error("Toss API error in getPaymentInfo: code={}, message={}", e.getTossError().getCode(), e.getTossError().getMessage());
            // TODO: Map to error status if needed
        } catch (final IOException | InterruptedException e) {
            logger.error("Network error in getPaymentInfo", e);
        }

        // If sync failed, return existing transactions
        return transactions;
    }

    @Override
    public Pagination<PaymentTransactionInfoPlugin> searchPayments(final String searchKey, final Long offset, final Long limit, final Iterable<PluginProperty> properties, final TenantContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public void addPaymentMethod(final UUID kbAccountId, final UUID kbPaymentMethodId, final PaymentMethodPlugin paymentMethodProps, final boolean setDefault, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        // TODO: Implement adding payment method
    }

    @Override
    public void deletePaymentMethod(final UUID kbAccountId, final UUID kbPaymentMethodId, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        // TODO: Implement deleting payment method
    }

    @Override
    public PaymentMethodPlugin getPaymentMethodDetail(final UUID kbAccountId, final UUID kbPaymentMethodId, final Iterable<PluginProperty> properties, final TenantContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public void setDefaultPaymentMethod(final UUID kbAccountId, final UUID kbPaymentMethodId, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
    }

    @Override
    public List<PaymentMethodInfoPlugin> getPaymentMethods(final UUID kbAccountId, final boolean refreshFromGateway, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return Collections.emptyList();
    }

    @Override
    public Pagination<PaymentMethodPlugin> searchPaymentMethods(final String searchKey, final Long offset, final Long limit, final Iterable<PluginProperty> properties, final TenantContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public void resetPaymentMethods(final UUID kbAccountId, final List<PaymentMethodInfoPlugin> paymentMethods, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
    }

    @Override
    public HostedPaymentPageFormDescriptor buildFormDescriptor(final UUID kbAccountId, final Iterable<PluginProperty> customFields, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public GatewayNotification processNotification(final String notification, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    protected PaymentTransactionInfoPlugin buildPaymentTransactionInfoPlugin(TossResponsesRecord record) {
        if (record == null) {
            return null;
        }
        return buildPaymentTransactionInfoFromRecord(UUID.fromString(record.getKbPaymentId()), record);
    }

    @Override
    protected PaymentMethodPlugin buildPaymentMethodPlugin(TossPaymentMethodsRecord record) {
        return null; // TODO: Implement
    }

    @Override
    protected PaymentMethodInfoPlugin buildPaymentMethodInfoPlugin(TossPaymentMethodsRecord record) {
        return null; // TODO: Implement
    }

    @Override
    protected String getPaymentMethodId(TossPaymentMethodsRecord record) {
        return null; // TODO: Implement
    }

    /**
     * Build successful payment response from Toss payment.
     */
    private PaymentTransactionInfoPlugin buildPaymentTransactionInfo(final UUID kbPaymentId,
                                                                      final UUID kbTransactionId,
                                                                      final TransactionType transactionType,
                                                                      final BigDecimal amount,
                                                                      final Currency currency,
                                                                      final TossPayment tossPayment) {
        final PaymentPluginStatus status = mapTossStatusToKillBill(tossPayment.getStatus());
        final DateTime now = clock.getUTCNow();

        return new TossPaymentTransactionInfoPlugin(
                kbPaymentId,
                kbTransactionId,
                transactionType,
                amount,
                currency,
                status,
                null, // gatewayError
                null, // gatewayErrorCode
                tossPayment.getPaymentKey(),
                tossPayment.getOrderId(),
                now,
                now,
                Collections.emptyList()
        );
    }

    private PaymentTransactionInfoPlugin buildRefundTransactionInfo(final UUID kbPaymentId,
                                                                     final UUID kbTransactionId,
                                                                     final BigDecimal amount,
                                                                     final Currency currency,
                                                                     final TossPayment tossPayment) {
        final PaymentPluginStatus status = mapTossStatusToKillBill(tossPayment.getStatus());
        final DateTime now = clock.getUTCNow();

        final String cancelTransactionKey = extractLatestCancelTransactionKey(tossPayment);

        return new TossPaymentTransactionInfoPlugin(
                kbPaymentId,
                kbTransactionId,
                TransactionType.REFUND,
                amount,
                currency,
                status,
                null,
                null,
                tossPayment.getPaymentKey(),
                cancelTransactionKey,
                now,
                now,
                Collections.emptyList()
        );
    }

    private String extractLatestCancelTransactionKey(final TossPayment tossPayment) {
        if (tossPayment.getCancels() == null || tossPayment.getCancels().isEmpty()) {
            return null;
        }
        return tossPayment.getCancels().get(tossPayment.getCancels().size() - 1).getTransactionKey();
    }

    private PaymentTransactionInfoPlugin buildErrorResponse(final UUID kbPaymentId,
                                                            final UUID kbTransactionId,
                                                            final TransactionType transactionType,
                                                            final BigDecimal amount,
                                                            final Currency currency,
                                                            final TossApplicationException e) {
        final PaymentPluginStatus status = mapTossErrorToStatus(e);
        final DateTime now = clock.getUTCNow();

        return new TossPaymentTransactionInfoPlugin(
                kbPaymentId,
                kbTransactionId,
                transactionType,
                amount,
                currency,
                status,
                e.getTossError().getMessage(),
                e.getTossError().getCode(),
                null, // firstPaymentReferenceId
                null, // secondPaymentReferenceId
                now,
                now,
                Collections.emptyList()
        );
    }

    /**
     * Build pending response for network errors.
     */
    private PaymentTransactionInfoPlugin buildPendingResponse(final UUID kbPaymentId,
                                                              final UUID kbTransactionId,
                                                              final TransactionType transactionType,
                                                              final BigDecimal amount,
                                                              final Currency currency,
                                                              final String paymentKey, // Add paymentKey
                                                              final Exception e) {
        final DateTime now = clock.getUTCNow();

        return new TossPaymentTransactionInfoPlugin(
                kbPaymentId,
                kbTransactionId,
                transactionType,
                amount,
                currency,
                PaymentPluginStatus.PENDING,
                e.getMessage(),
                "NETWORK_ERROR",
                paymentKey, // Include paymentKey so we can check status later
                null,
                now,
                now,
                Collections.emptyList()
        );
    }

    /**
     * Map Toss payment status to Kill Bill status.
     */
    private PaymentPluginStatus mapTossStatusToKillBill(final String tossStatus) {
        if (tossStatus == null) {
            return PaymentPluginStatus.PENDING;
        }

        switch (tossStatus) {
            case "DONE":
                return PaymentPluginStatus.PROCESSED;
            case "IN_PROGRESS":
            case "WAITING_FOR_DEPOSIT":
            case "PARTIAL_CANCELED":
                return PaymentPluginStatus.PENDING;
            case "CANCELED":
            case "ABORTED":
                return PaymentPluginStatus.CANCELED;
            default:
                logger.warn("Unknown Toss status: {}, defaulting to PENDING", tossStatus);
                return PaymentPluginStatus.PENDING;
        }
    }

    /**
     * Map Toss error code to Kill Bill status (FR-07).
     * - 5xx errors → PENDING (retryable)
     * - 4xx errors → ERROR (not retryable)
     */
    private PaymentPluginStatus mapTossErrorToStatus(final TossApplicationException e) {
        final String errorCode = e.getTossError().getCode();
        final int statusCode = e.getStatusCode();

        // 5xx errors or specific retryable error codes → PENDING
        if (statusCode >= 500 || isRetryableErrorCode(errorCode)) {
            return PaymentPluginStatus.PENDING;
        }

        // 4xx errors → ERROR
        return PaymentPluginStatus.ERROR;
    }

    /**
     * Check if Toss error code is retryable (should return PENDING).
     */
    private boolean isRetryableErrorCode(final String errorCode) {
        if (errorCode == null) {
            return false;
        }

        // 5xx-like error codes (processing errors)
        return errorCode.startsWith("FAILED_") && errorCode.contains("PROCESSING") ||
               errorCode.equals("PROVIDER_ERROR") ||
               errorCode.equals("COMMON_ERROR");
    }

    /**
     * Build PaymentTransactionInfoPlugin from database record.
     */
    private PaymentTransactionInfoPlugin buildPaymentTransactionInfoFromRecord(final UUID kbPaymentId,
                                                                                final TossResponsesRecord record) {
        final PaymentPluginStatus status = mapTossStatusToKillBill(record.getTossPaymentStatus());
        final DateTime createdDate = new DateTime(
                record.getCreatedDate().atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
                org.joda.time.DateTimeZone.UTC
        );

        return new TossPaymentTransactionInfoPlugin(
                kbPaymentId,
                UUID.fromString(record.getKbPaymentTransactionId()),
                TransactionType.valueOf(record.getTransactionType()),
                record.getAmount(),
                record.getCurrency() == null ? null : Currency.valueOf(record.getCurrency()),
                status,
                null, // gatewayError
                null, // gatewayErrorCode
                record.getPaymentKey(), // firstPaymentReferenceId
                record.getOrderId(), // secondPaymentReferenceId
                createdDate,
                createdDate,
                Collections.emptyList()
        );
    }
}
