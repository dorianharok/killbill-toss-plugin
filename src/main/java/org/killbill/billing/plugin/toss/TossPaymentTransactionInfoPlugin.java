package org.killbill.billing.plugin.toss;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.joda.time.DateTime;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;
import org.killbill.billing.plugin.api.payment.PluginPaymentTransactionInfoPlugin;

public class TossPaymentTransactionInfoPlugin extends PluginPaymentTransactionInfoPlugin {

    public TossPaymentTransactionInfoPlugin(final UUID kbPaymentId,
                                            final UUID kbTransactionPaymentId,
                                            final TransactionType transactionType,
                                            final BigDecimal amount,
                                            final Currency currency,
                                            final PaymentPluginStatus status,
                                            final String gatewayError,
                                            final String gatewayErrorCode,
                                            final String firstPaymentReferenceId,
                                            final String secondPaymentReferenceId,
                                            final DateTime createdDate,
                                            final DateTime effectiveDate,
                                            final List<PluginProperty> properties) {
        super(kbPaymentId,
              kbTransactionPaymentId,
              transactionType,
              amount,
              currency,
              status,
              gatewayError,
              gatewayErrorCode,
              firstPaymentReferenceId,
              secondPaymentReferenceId,
              createdDate,
              effectiveDate,
              properties);
    }

    /**
     * Create a response for an unimplemented API operation.
     * Returns CANCELED status with an error message.
     */
    public static TossPaymentTransactionInfoPlugin unimplementedAPI(final UUID kbPaymentId,
                                                                    final UUID kbTransactionId,
                                                                    final TransactionType transactionType,
                                                                    final String operationName) {
        return new TossPaymentTransactionInfoPlugin(
                kbPaymentId,
                kbTransactionId,
                transactionType,
                null,
                null,
                PaymentPluginStatus.CANCELED,
                "Operation not supported: " + operationName,
                "UNSUPPORTED_OPERATION",
                null,
                null,
                DateTime.now(),
                DateTime.now(),
                Collections.emptyList()
        );
    }
}
