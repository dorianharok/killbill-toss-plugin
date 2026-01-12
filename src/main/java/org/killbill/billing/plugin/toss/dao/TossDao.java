package org.killbill.billing.plugin.toss.dao;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.sql.DataSource;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.joda.time.DateTime;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.plugin.dao.payment.PluginPaymentDao;
import org.killbill.billing.plugin.toss.client.exception.TossApplicationException;
import org.killbill.billing.plugin.toss.client.model.TossPayment;
import org.killbill.billing.plugin.toss.dao.gen.tables.TossPaymentMethods;
import org.killbill.billing.plugin.toss.dao.gen.tables.TossResponses;
import org.killbill.billing.plugin.toss.dao.gen.tables.records.TossPaymentMethodsRecord;
import org.killbill.billing.plugin.toss.dao.gen.tables.records.TossResponsesRecord;

import org.killbill.billing.plugin.toss.client.model.TossBilling;

import static org.killbill.billing.plugin.toss.dao.gen.tables.TossPaymentMethods.TOSS_PAYMENT_METHODS;
import static org.killbill.billing.plugin.toss.dao.gen.tables.TossResponses.TOSS_RESPONSES;

public class TossDao extends PluginPaymentDao<TossResponsesRecord, TossResponses, TossPaymentMethodsRecord, TossPaymentMethods> {

    public static final short TRUE = 1;
    public static final short FALSE = 0;

    public TossDao(final DataSource dataSource) throws SQLException {
        super(new TossResponses(), new TossPaymentMethods(), dataSource);
        // Save space in the database
        objectMapper.setSerializationInclusion(Include.NON_EMPTY);
    }

    /**
     * Add a response record for a payment.
     *
     * @param kbAccountId the Kill Bill account ID
     * @param kbPaymentId the Kill Bill payment ID
     * @param kbPaymentTransactionId the Kill Bill payment transaction ID
     * @param transactionType the transaction type
     * @param amount the payment amount
     * @param currency the payment currency
     * @param paymentKey the Toss payment key (required for tracking and recovery)
     * @param tossPayment the Toss payment response (can be null for errors)
     * @param tossException the Toss exception (can be null for success)
     * @param utcNow the current UTC time
     * @param kbTenantId the Kill Bill tenant ID
     * @return the created TossResponsesRecord
     * @throws SQLException if a database error occurs
     */
    public TossResponsesRecord addResponse(final UUID kbAccountId,
                                           final UUID kbPaymentId,
                                           final UUID kbPaymentTransactionId,
                                           final TransactionType transactionType,
                                           final BigDecimal amount,
                                           final Currency currency,
                                           final String paymentKey,
                                           @Nullable final TossPayment tossPayment,
                                           @Nullable final TossApplicationException tossException,
                                           final DateTime utcNow,
                                           final UUID kbTenantId) throws SQLException {
        final Map<String, Object> additionalDataMap;
        if (tossPayment != null) {
            additionalDataMap = toAdditionalDataMap(tossPayment);
        } else if (tossException != null) {
            additionalDataMap = toAdditionalDataMap(tossException);
        } else {
            additionalDataMap = Collections.emptyMap();
        }

        return execute(dataSource.getConnection(),
                       conn -> DSL.using(conn, dialect, settings).transactionResult(configuration -> {
                           final DSLContext dslContext = DSL.using(configuration);
                           dslContext.insertInto(TOSS_RESPONSES,
                                                 TOSS_RESPONSES.KB_ACCOUNT_ID,
                                                 TOSS_RESPONSES.KB_PAYMENT_ID,
                                                 TOSS_RESPONSES.KB_PAYMENT_TRANSACTION_ID,
                                                 TOSS_RESPONSES.TRANSACTION_TYPE,
                                                 TOSS_RESPONSES.AMOUNT,
                                                 TOSS_RESPONSES.CURRENCY,
                                                 TOSS_RESPONSES.PAYMENT_KEY,
                                                 TOSS_RESPONSES.ORDER_ID,
                                                 TOSS_RESPONSES.TOSS_PAYMENT_STATUS,
                                                 TOSS_RESPONSES.TOSS_METHOD,
                                                 TOSS_RESPONSES.TOSS_RECEIPT_URL,
                                                 TOSS_RESPONSES.ADDITIONAL_DATA,
                                                 TOSS_RESPONSES.CREATED_DATE,
                                                 TOSS_RESPONSES.KB_TENANT_ID)
                                      .values(kbAccountId.toString(),
                                              kbPaymentId.toString(),
                                   kbPaymentTransactionId == null ? null : kbPaymentTransactionId.toString(),
                                              transactionType.toString(),
                                              amount,
                                              currency == null ? null : currency.name(),
                                              paymentKey,
                                              tossPayment == null ? null : tossPayment.getOrderId(),
                                              tossPayment == null ? null : tossPayment.getStatus(),
                                              tossPayment == null ? null : tossPayment.getMethod(),
                                              extractReceiptUrl(tossPayment),
                                              asString(additionalDataMap),
                                              toLocalDateTime(utcNow),
                                              kbTenantId.toString())
                                      .execute();
                           return dslContext.fetchOne(
                                   TOSS_RESPONSES,
                                   TOSS_RESPONSES.RECORD_ID.eq(TOSS_RESPONSES.RECORD_ID.getDataType().convert(dslContext.lastID())));
                       }));
    }

    /**
     * Convert TossPayment to additional data map for JSON storage.
     */
    private Map<String, Object> toAdditionalDataMap(final TossPayment tossPayment) {
        try {
            // Convert TossPayment to JSON and then to Map
            final String json = objectMapper.writeValueAsString(tossPayment);
            return objectMapper.readValue(json, Map.class);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to serialize TossPayment", e);
        }
    }

    /**
     * Convert TossApplicationException to additional data map for JSON storage.
     */
    private Map<String, Object> toAdditionalDataMap(final TossApplicationException tossException) {
        try {
            final Map<String, Object> map = new java.util.HashMap<>();
            map.put("errorCode", tossException.getTossError().getCode());
            map.put("errorMessage", maskSensitiveData(tossException.getTossError().getMessage()));
            map.put("statusCode", tossException.getStatusCode());
            return map;
        } catch (final Exception e) {
            throw new RuntimeException("Failed to serialize TossApplicationException", e);
        }
    }

    /**
     * Extract receipt URL from TossPayment, handling null values.
     */
    private String extractReceiptUrl(@Nullable final TossPayment tossPayment) {
        if (tossPayment == null) {
            return null;
        }
        if (tossPayment.getReceipt() != null) {
            return tossPayment.getReceipt().getUrl();
        }
        if (tossPayment.getCard() != null) {
            return tossPayment.getCard().getReceiptUrl();
        }
        return null;
    }

    /**
     * Mask sensitive data in error messages (credit card numbers, etc.).
     */
    private String maskSensitiveData(final String message) {
        if (message == null) {
            return null;
        }
        // Mask credit card numbers (16 digits)
        String masked = message.replaceAll("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b", "****-****-****-****");
        // Mask any other long numeric sequences (potential card numbers)
        masked = masked.replaceAll("\\b\\d{13,19}\\b", "****");
        return masked;
    }

    /**
     * Get the response for a specific transaction.
     * Used for idempotency check - if a transaction with this ID was already processed,
     * return the existing result instead of processing again.
     *
     * @param kbTransactionId the Kill Bill payment transaction ID
     * @param kbTenantId the Kill Bill tenant ID
     * @return the TossResponsesRecord for this transaction or null if not found
     * @throws SQLException if a database error occurs
     */
    public TossResponsesRecord getResponse(final UUID kbTransactionId,
                                           final UUID kbTenantId) throws SQLException {
        return execute(dataSource.getConnection(),
                       new WithConnectionCallback<TossResponsesRecord>() {
                           @Override
                           public TossResponsesRecord withConnection(final Connection conn) throws SQLException {
                               return DSL.using(conn, dialect, settings)
                                         .selectFrom(TOSS_RESPONSES)
                                         .where(TOSS_RESPONSES.KB_PAYMENT_TRANSACTION_ID.equal(kbTransactionId.toString()))
                                         .and(TOSS_RESPONSES.KB_TENANT_ID.equal(kbTenantId.toString()))
                                         .orderBy(TOSS_RESPONSES.RECORD_ID.desc())
                                         .limit(1)
                                         .fetchOne();
                           }
                       });
    }

    /**
     * Get the most recent response for a payment.
     *
     * @param kbPaymentId the Kill Bill payment ID
     * @param kbTenantId the Kill Bill tenant ID
     * @return the most recent TossResponsesRecord or null if not found
     * @throws SQLException if a database error occurs
     */
    public TossResponsesRecord getResponseByPaymentId(final UUID kbPaymentId,
                                                      final UUID kbTenantId) throws SQLException {
        return execute(dataSource.getConnection(),
                       new WithConnectionCallback<TossResponsesRecord>() {
                           @Override
                           public TossResponsesRecord withConnection(final Connection conn) throws SQLException {
                               return DSL.using(conn, dialect, settings)
                                         .selectFrom(TOSS_RESPONSES)
                                         .where(TOSS_RESPONSES.KB_PAYMENT_ID.equal(kbPaymentId.toString()))
                                         .and(TOSS_RESPONSES.KB_TENANT_ID.equal(kbTenantId.toString()))
                                         .orderBy(TOSS_RESPONSES.RECORD_ID.desc())
                                         .limit(1)
                                         .fetchOne();
                           }
                       });
    }

    /**
     * Get the original successful PURCHASE response for a payment.
     * Used by refund to validate against the original payment amount.
     *
     * @param kbPaymentId the Kill Bill payment ID
     * @param kbTenantId the Kill Bill tenant ID
     * @return the original PURCHASE TossResponsesRecord or null if not found
     * @throws SQLException if a database error occurs
     */
    public TossResponsesRecord getSuccessfulPurchaseResponse(final UUID kbPaymentId,
                                                             final UUID kbTenantId) throws SQLException {
        return execute(dataSource.getConnection(),
                       new WithConnectionCallback<TossResponsesRecord>() {
                           @Override
                           public TossResponsesRecord withConnection(final Connection conn) throws SQLException {
                               return DSL.using(conn, dialect, settings)
                                         .selectFrom(TOSS_RESPONSES)
                                         .where(TOSS_RESPONSES.KB_PAYMENT_ID.equal(kbPaymentId.toString()))
                                         .and(TOSS_RESPONSES.KB_TENANT_ID.equal(kbTenantId.toString()))
                                         .and(TOSS_RESPONSES.TRANSACTION_TYPE.equal(TransactionType.PURCHASE.toString()))
                                         .and(TOSS_RESPONSES.TOSS_PAYMENT_STATUS.in("DONE", "PARTIAL_CANCELED"))
                                         .orderBy(TOSS_RESPONSES.RECORD_ID.desc())
                                         .limit(1)
                                         .fetchOne();
                           }
                       });
    }

    /**
     * Deserialize additional data from JSON string to Map.
     */
    public static Map fromAdditionalData(@Nullable final String additionalData) {
        if (additionalData == null) {
            return Collections.emptyMap();
        }

        try {
            return objectMapper.readValue(additionalData, Map.class);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addPaymentMethod(final UUID kbAccountId,
                                 final UUID kbPaymentMethodId,
                                 final boolean isDefault,
                                 final TossBilling tossBilling,
                                 final DateTime utcNow,
                                 final UUID kbTenantId) throws SQLException {
        final Map<String, Object> additionalDataMap = toAdditionalDataMap(tossBilling);

        execute(dataSource.getConnection(),
                conn -> DSL.using(conn, dialect, settings).transactionResult(configuration -> {
                    final DSLContext dslContext = DSL.using(configuration);
                    dslContext.insertInto(TOSS_PAYMENT_METHODS,
                                          TOSS_PAYMENT_METHODS.KB_ACCOUNT_ID,
                                          TOSS_PAYMENT_METHODS.KB_PAYMENT_METHOD_ID,
                                          TOSS_PAYMENT_METHODS.BILLING_KEY,
                                          TOSS_PAYMENT_METHODS.CUSTOMER_KEY,
                                          TOSS_PAYMENT_METHODS.IS_DEFAULT,
                                          TOSS_PAYMENT_METHODS.IS_DELETED,
                                          TOSS_PAYMENT_METHODS.ADDITIONAL_DATA,
                                          TOSS_PAYMENT_METHODS.CREATED_DATE,
                                          TOSS_PAYMENT_METHODS.UPDATED_DATE,
                                          TOSS_PAYMENT_METHODS.KB_TENANT_ID)
                               .values(kbAccountId.toString(),
                                       kbPaymentMethodId.toString(),
                                       tossBilling.getBillingKey(),
                                       tossBilling.getCustomerKey(),
                                       isDefault ? TRUE : FALSE,
                                       FALSE,
                                       asString(additionalDataMap),
                                       toLocalDateTime(utcNow),
                                       toLocalDateTime(utcNow),
                                       kbTenantId.toString())
                               .execute();
                    return null;
                }));
    }

    public TossPaymentMethodsRecord getPaymentMethod(final UUID kbPaymentMethodId,
                                                     final UUID kbTenantId) throws SQLException {
        return execute(dataSource.getConnection(),
                       conn -> DSL.using(conn, dialect, settings)
                                  .selectFrom(TOSS_PAYMENT_METHODS)
                                  .where(TOSS_PAYMENT_METHODS.KB_PAYMENT_METHOD_ID.equal(kbPaymentMethodId.toString()))
                                  .and(TOSS_PAYMENT_METHODS.KB_TENANT_ID.equal(kbTenantId.toString()))
                                  .and(TOSS_PAYMENT_METHODS.IS_DELETED.equal(FALSE))
                                  .fetchOne());
    }

    public List<TossPaymentMethodsRecord> getPaymentMethods(final UUID kbAccountId,
                                                            final UUID kbTenantId) throws SQLException {
        return execute(dataSource.getConnection(),
                       conn -> DSL.using(conn, dialect, settings)
                                  .selectFrom(TOSS_PAYMENT_METHODS)
                                  .where(TOSS_PAYMENT_METHODS.KB_ACCOUNT_ID.equal(kbAccountId.toString()))
                                  .and(TOSS_PAYMENT_METHODS.KB_TENANT_ID.equal(kbTenantId.toString()))
                                  .and(TOSS_PAYMENT_METHODS.IS_DELETED.equal(FALSE))
                                  .fetch());
    }

    public void deletePaymentMethod(final UUID kbPaymentMethodId,
                                    final UUID kbTenantId) throws SQLException {
        execute(dataSource.getConnection(),
                conn -> DSL.using(conn, dialect, settings)
                           .update(TOSS_PAYMENT_METHODS)
                           .set(TOSS_PAYMENT_METHODS.IS_DELETED, TRUE)
                           .where(TOSS_PAYMENT_METHODS.KB_PAYMENT_METHOD_ID.equal(kbPaymentMethodId.toString()))
                           .and(TOSS_PAYMENT_METHODS.KB_TENANT_ID.equal(kbTenantId.toString()))
                           .execute());
    }

    public void setDefaultPaymentMethod(final UUID kbPaymentMethodId,
                                        final UUID kbAccountId,
                                        final UUID kbTenantId) throws SQLException {
        execute(dataSource.getConnection(),
                conn -> DSL.using(conn, dialect, settings).transactionResult(configuration -> {
                    final DSLContext dslContext = DSL.using(configuration);
                    dslContext.update(TOSS_PAYMENT_METHODS)
                              .set(TOSS_PAYMENT_METHODS.IS_DEFAULT, FALSE)
                              .where(TOSS_PAYMENT_METHODS.KB_ACCOUNT_ID.equal(kbAccountId.toString()))
                              .and(TOSS_PAYMENT_METHODS.KB_TENANT_ID.equal(kbTenantId.toString()))
                              .execute();
                    dslContext.update(TOSS_PAYMENT_METHODS)
                              .set(TOSS_PAYMENT_METHODS.IS_DEFAULT, TRUE)
                              .where(TOSS_PAYMENT_METHODS.KB_PAYMENT_METHOD_ID.equal(kbPaymentMethodId.toString()))
                              .and(TOSS_PAYMENT_METHODS.KB_TENANT_ID.equal(kbTenantId.toString()))
                              .execute();
                    return null;
                }));
    }

    private Map<String, Object> toAdditionalDataMap(final TossBilling tossBilling) {
        try {
            final String json = objectMapper.writeValueAsString(tossBilling);
            return objectMapper.readValue(json, Map.class);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to serialize TossBilling", e);
        }
    }
}
