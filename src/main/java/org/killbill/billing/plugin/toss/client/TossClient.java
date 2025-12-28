package org.killbill.billing.plugin.toss.client;

import org.killbill.billing.plugin.toss.client.model.BillingKeyPaymentRequest;
import org.killbill.billing.plugin.toss.client.model.BillingKeyRequest;
import org.killbill.billing.plugin.toss.client.model.PaymentCancelRequest;
import org.killbill.billing.plugin.toss.client.model.PaymentConfirmRequest;
import org.killbill.billing.plugin.toss.client.model.TossBilling;
import org.killbill.billing.plugin.toss.client.model.TossPayment;

import java.io.IOException;

public interface TossClient {

    /**
     * Confirms a payment with the given request data.
     *
     * @param secretKey the secret key for authentication
     * @param request the payment confirmation request data
     * @param idempotencyKey unique key to prevent duplicate confirmations (e.g., kbTransactionId)
     * @return the confirmed payment
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    TossPayment confirmPayment(String secretKey, PaymentConfirmRequest request, String idempotencyKey) throws IOException, InterruptedException;

    /**
     * Cancels a payment.
     *
     * @param secretKey the secret key for authentication
     * @param paymentKey the unique key of the payment to cancel
     * @param request the cancel request data
     * @param idempotencyKey unique key to prevent duplicate cancellations (e.g., kbTransactionId)
     * @return the cancelled payment information
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    TossPayment cancelPayment(String secretKey, String paymentKey, PaymentCancelRequest request, String idempotencyKey) throws IOException, InterruptedException;

    /**
     * Retrieves payment information by payment key.
     *
     * @param secretKey the secret key for authentication
     * @param paymentKey the unique key of the payment
     * @return the payment information
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    TossPayment getPayment(String secretKey, String paymentKey) throws IOException, InterruptedException;

    /**
     * Issues a billing key for automatic payments.
     *
     * @param secretKey the secret key for authentication
     * @param request the billing key issuance request data
     * @return the billing key information
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    TossBilling issueBillingKey(String secretKey, BillingKeyRequest request) throws IOException, InterruptedException;

    /**
     * Executes a payment using a billing key.
     *
     * @param secretKey the secret key for authentication
     * @param billingKey the billing key for automatic payment
     * @param request the billing key payment request data
     * @param idempotencyKey unique key to prevent duplicate payments (e.g., kbTransactionId)
     * @return the payment result
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    TossPayment executeBillingKeyPayment(String secretKey, String billingKey, BillingKeyPaymentRequest request, String idempotencyKey) throws IOException, InterruptedException;
}
