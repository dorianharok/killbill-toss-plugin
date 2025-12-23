package org.killbill.billing.plugin.toss;

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

        try {
            // Extract Toss parameters from plugin properties
            final String paymentKey = PluginProperties.findPluginPropertyValue("paymentKey", properties);
            if (paymentKey == null) {
                throw new PaymentPluginApiException("ERROR", "Missing required property: paymentKey");
            }

            final String orderId = PluginProperties.getValue("orderId", kbPaymentId.toString(), properties);

            final Long tossAmount = amount.multiply(BigDecimal.valueOf(100))
                    .longValue(); // Convert to cents/won

            // Get tenant configuration
            final TossConfigProperties config = getConfigForTenant(context);
            final String secretKey = config.getSecretKey();

            // Call Toss API
            final PaymentConfirmRequest request = new PaymentConfirmRequest(paymentKey, orderId, tossAmount);
            final TossPayment tossPayment = tossClient.confirmPayment(secretKey, request);

            // Build success response
            final PaymentTransactionInfoPlugin response = buildPaymentTransactionInfo(
                    kbPaymentId,
                    kbTransactionId,
                    TransactionType.PURCHASE,
                    amount,
                    currency,
                    tossPayment
            );

            // Save to database (will be implemented in Task 3)
            // dao.addResponse(kbAccountId, kbPaymentId, kbTransactionId, transactionType, amount, currency, tossPayment, context);

            logger.info("purchasePayment succeeded: paymentKey={}, status={}", paymentKey, tossPayment.getStatus());
            return response;

        } catch (final TossApplicationException e) {
            logger.error("Toss API error: code={}, message={}", e.getTossError().getCode(), e.getTossError().getMessage());
            return buildErrorResponse(kbPaymentId, kbTransactionId, TransactionType.PURCHASE, amount, currency, e);

        } catch (final IOException | InterruptedException e) {
            logger.error("Network error during payment confirmation", e);
            return buildPendingResponse(kbPaymentId, kbTransactionId, TransactionType.PURCHASE, amount, currency, e);
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
        // TODO: Implement Toss Payments refund
        return null;
    }

    @Override
    public List<PaymentTransactionInfoPlugin> getPaymentInfo(final UUID kbAccountId, final UUID kbPaymentId, final Iterable<PluginProperty> properties, final TenantContext context) throws PaymentPluginApiException {
        logger.info("getPaymentInfo called: kbPaymentId={}", kbPaymentId);

        try {
            // Try to get paymentKey from plugin properties
            final String paymentKey = PluginProperties.findPluginPropertyValue("paymentKey", properties);

            if (paymentKey == null) {
                // If no paymentKey provided, try to get from database (will be implemented in Task 3)
                logger.warn("No paymentKey provided in properties for kbPaymentId={}", kbPaymentId);
                return Collections.emptyList();
            }

            // Get tenant configuration
            final TossConfigProperties config = getConfigForTenant(context);
            final String secretKey = config.getSecretKey();

            // Call Toss API to get latest payment status
            final TossPayment tossPayment = tossClient.getPayment(secretKey, paymentKey);

            // Build response
            final PaymentTransactionInfoPlugin response = buildPaymentTransactionInfo(
                    kbPaymentId,
                    null, // kbTransactionId not available here
                    TransactionType.PURCHASE, // Assume PURCHASE for now
                    BigDecimal.valueOf(tossPayment.getTotalAmount()).divide(BigDecimal.valueOf(100)),
                    Currency.valueOf(tossPayment.getCurrency()),
                    tossPayment
            );

            logger.info("getPaymentInfo succeeded: paymentKey={}, status={}", paymentKey, tossPayment.getStatus());
            return Collections.singletonList(response);

        } catch (final TossApplicationException e) {
            logger.error("Toss API error in getPaymentInfo: code={}, message={}",
                    e.getTossError().getCode(), e.getTossError().getMessage());
            return Collections.emptyList();

        } catch (final IOException | InterruptedException e) {
            logger.error("Network error in getPaymentInfo", e);
            return Collections.emptyList();
        }
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
        return null; // TODO: Implement
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
        final DateTime now = DateTime.now();

        return new TossPaymentTransactionInfoPlugin(
                kbPaymentId,
                kbTransactionId,
                transactionType,
                amount,
                currency,
                status,
                null, // gatewayError
                null, // gatewayErrorCode
                tossPayment.getPaymentKey(), // firstPaymentReferenceId
                tossPayment.getOrderId(), // secondPaymentReferenceId
                now,
                now,
                Collections.emptyList()
        );
    }

    /**
     * Build error response from Toss application exception.
     */
    private PaymentTransactionInfoPlugin buildErrorResponse(final UUID kbPaymentId,
                                                            final UUID kbTransactionId,
                                                            final TransactionType transactionType,
                                                            final BigDecimal amount,
                                                            final Currency currency,
                                                            final TossApplicationException e) {
        final PaymentPluginStatus status = mapTossErrorToStatus(e);
        final DateTime now = DateTime.now();

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
                                                              final Exception e) {
        final DateTime now = DateTime.now();

        return new TossPaymentTransactionInfoPlugin(
                kbPaymentId,
                kbTransactionId,
                transactionType,
                amount,
                currency,
                PaymentPluginStatus.PENDING,
                e.getMessage(),
                "NETWORK_ERROR",
                null,
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
}
