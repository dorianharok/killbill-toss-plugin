/*
 * Copyright 2024 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.toss.api;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.toss.TestBase;
import org.killbill.billing.plugin.toss.client.exception.TossApplicationException;
import org.killbill.billing.plugin.toss.client.model.TossBilling;
import org.killbill.billing.plugin.toss.client.model.TossError;
import org.killbill.billing.plugin.toss.client.model.TossPayment;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

public class TossPaymentPluginApiTest extends TestBase {

    @Test(groups = "slow")
    public void testAuthorizePayment_ReturnsCANCELED() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();

        final PaymentTransactionInfoPlugin result = tossPaymentPluginApi.authorizePayment(
                account.getId(),
                kbPaymentId,
                kbTransactionId,
                account.getPaymentMethodId(),
                BigDecimal.valueOf(1000),
                Currency.KRW,
                Collections.emptyList(),
                context
        );

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getStatus(), PaymentPluginStatus.CANCELED);
        Assert.assertEquals(result.getGatewayErrorCode(), "UNSUPPORTED_OPERATION");
        Assert.assertTrue(result.getGatewayError().contains("authorizePayment"));
    }

    @Test(groups = "slow")
    public void testCapturePayment_ReturnsCANCELED() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();

        final PaymentTransactionInfoPlugin result = tossPaymentPluginApi.capturePayment(
                account.getId(),
                kbPaymentId,
                kbTransactionId,
                account.getPaymentMethodId(),
                BigDecimal.valueOf(1000),
                Currency.KRW,
                Collections.emptyList(),
                context
        );

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getStatus(), PaymentPluginStatus.CANCELED);
        Assert.assertEquals(result.getGatewayErrorCode(), "UNSUPPORTED_OPERATION");
    }

    @Test(groups = "slow")
    public void testVoidPayment_ReturnsCANCELED() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();

        final PaymentTransactionInfoPlugin result = tossPaymentPluginApi.voidPayment(
                account.getId(),
                kbPaymentId,
                kbTransactionId,
                account.getPaymentMethodId(),
                Collections.emptyList(),
                context
        );

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getStatus(), PaymentPluginStatus.CANCELED);
        Assert.assertEquals(result.getGatewayErrorCode(), "UNSUPPORTED_OPERATION");
    }

    @Test(groups = "slow")
    public void testCreditPayment_ReturnsCANCELED() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();

        final PaymentTransactionInfoPlugin result = tossPaymentPluginApi.creditPayment(
                account.getId(),
                kbPaymentId,
                kbTransactionId,
                account.getPaymentMethodId(),
                BigDecimal.valueOf(1000),
                Currency.KRW,
                Collections.emptyList(),
                context
        );

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getStatus(), PaymentPluginStatus.CANCELED);
        Assert.assertEquals(result.getGatewayErrorCode(), "UNSUPPORTED_OPERATION");
    }

    @Test(groups = "slow")
    public void testPurchasePayment_Success() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();
        final String paymentKey = "test_payment_key_123";
        final String orderId = kbPaymentId.toString();
        final BigDecimal amount = BigDecimal.valueOf(10000);

        final TossPayment mockPayment = createMockTossPayment(paymentKey, orderId, 10000L, "DONE");
        Mockito.when(tossClient.confirmPayment(Mockito.anyString(), Mockito.any(), Mockito.anyString()))
               .thenReturn(mockPayment);

        final List<PluginProperty> properties = ImmutableList.of(
                new PluginProperty("paymentKey", paymentKey, false)
        );

        final PaymentTransactionInfoPlugin result = tossPaymentPluginApi.purchasePayment(
                account.getId(),
                kbPaymentId,
                kbTransactionId,
                account.getPaymentMethodId(),
                amount,
                Currency.KRW,
                properties,
                context
        );

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getStatus(), PaymentPluginStatus.PROCESSED);
        Assert.assertEquals(result.getFirstPaymentReferenceId(), paymentKey);
        Assert.assertEquals(result.getSecondPaymentReferenceId(), orderId);
        Assert.assertEquals(result.getKbPaymentId(), kbPaymentId);
        Assert.assertEquals(result.getKbTransactionPaymentId(), kbTransactionId);
    }

    @Test(groups = "slow")
    public void testPurchasePayment_MissingPaymentKey_ThrowsError() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();

        try {
            tossPaymentPluginApi.purchasePayment(
                    account.getId(),
                    kbPaymentId,
                    kbTransactionId,
                    account.getPaymentMethodId(),
                    BigDecimal.valueOf(10000),
                    Currency.KRW,
                    Collections.emptyList(),
                    context
            );
            Assert.fail("Should throw PaymentPluginApiException for missing paymentKey");
        } catch (final PaymentPluginApiException e) {
            Assert.assertNotNull(e);
        }
    }

    @Test(groups = "slow")
    public void testPurchasePayment_4xxError_ReturnsERROR() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();
        final String paymentKey = "test_payment_key_123";

        final TossError tossError = new TossError("INVALID_REQUEST", "Invalid request parameters");
        final TossApplicationException exception = new TossApplicationException(tossError, 400);
        Mockito.when(tossClient.confirmPayment(Mockito.anyString(), Mockito.any(), Mockito.anyString()))
               .thenThrow(exception);

        final List<PluginProperty> properties = ImmutableList.of(
                new PluginProperty("paymentKey", paymentKey, false)
        );

        final PaymentTransactionInfoPlugin result = tossPaymentPluginApi.purchasePayment(
                account.getId(),
                kbPaymentId,
                kbTransactionId,
                account.getPaymentMethodId(),
                BigDecimal.valueOf(10000),
                Currency.KRW,
                properties,
                context
        );

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getStatus(), PaymentPluginStatus.ERROR);
        Assert.assertEquals(result.getGatewayErrorCode(), "INVALID_REQUEST");
        Assert.assertNotNull(result.getGatewayError());
    }

    @Test(groups = "slow")
    public void testPurchasePayment_5xxError_ReturnsPENDING() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();
        final String paymentKey = "test_payment_key_123";

        final TossError tossError = new TossError("FAILED_INTERNAL_SYSTEM_PROCESSING", "Internal server error");
        final TossApplicationException exception = new TossApplicationException(tossError, 500);
        Mockito.when(tossClient.confirmPayment(Mockito.anyString(), Mockito.any(), Mockito.anyString()))
               .thenThrow(exception);

        final List<PluginProperty> properties = ImmutableList.of(
                new PluginProperty("paymentKey", paymentKey, false)
        );

        final PaymentTransactionInfoPlugin result = tossPaymentPluginApi.purchasePayment(
                account.getId(),
                kbPaymentId,
                kbTransactionId,
                account.getPaymentMethodId(),
                BigDecimal.valueOf(10000),
                Currency.KRW,
                properties,
                context
        );

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getStatus(), PaymentPluginStatus.PENDING);
    }

    @Test(groups = "slow")
    public void testPurchasePayment_NetworkError_ReturnsPENDING() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();
        final String paymentKey = "test_payment_key_123";

        Mockito.when(tossClient.confirmPayment(Mockito.anyString(), Mockito.any(), Mockito.anyString()))
               .thenThrow(new IOException("Connection timeout"));

        final List<PluginProperty> properties = ImmutableList.of(
                new PluginProperty("paymentKey", paymentKey, false)
        );

        final PaymentTransactionInfoPlugin result = tossPaymentPluginApi.purchasePayment(
                account.getId(),
                kbPaymentId,
                kbTransactionId,
                account.getPaymentMethodId(),
                BigDecimal.valueOf(10000),
                Currency.KRW,
                properties,
                context
        );

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getStatus(), PaymentPluginStatus.PENDING);
        Assert.assertEquals(result.getGatewayErrorCode(), "NETWORK_ERROR");
        Assert.assertEquals(result.getFirstPaymentReferenceId(), paymentKey);
    }

    @Test(groups = "slow")
    public void testPurchasePayment_Success_SavesToDB() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();
        final String paymentKey = "test_payment_key_db";
        final String orderId = kbPaymentId.toString();

        final TossPayment mockPayment = createMockTossPayment(paymentKey, orderId, 10000L, "DONE");
        Mockito.when(tossClient.confirmPayment(Mockito.anyString(), Mockito.any(), Mockito.anyString()))
               .thenReturn(mockPayment);

        final List<PluginProperty> properties = ImmutableList.of(
                new PluginProperty("paymentKey", paymentKey, false)
        );

        tossPaymentPluginApi.purchasePayment(
                account.getId(),
                kbPaymentId,
                kbTransactionId,
                account.getPaymentMethodId(),
                BigDecimal.valueOf(10000),
                Currency.KRW,
                properties,
                context
        );

        final var dbRecord = dao.getResponseByPaymentId(kbPaymentId, context.getTenantId());
        Assert.assertNotNull(dbRecord);
        Assert.assertEquals(dbRecord.getPaymentKey(), paymentKey);
        Assert.assertEquals(dbRecord.getOrderId(), orderId);
        Assert.assertEquals(dbRecord.getTossPaymentStatus(), "DONE");
    }

    @Test(groups = "slow")
    public void testPurchasePayment_NetworkError_SavesPaymentKeyToDB() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();
        final String paymentKey = "test_payment_key_network_error";

        Mockito.when(tossClient.confirmPayment(Mockito.anyString(), Mockito.any(), Mockito.anyString()))
               .thenThrow(new IOException("Connection timeout"));

        final List<PluginProperty> properties = ImmutableList.of(
                new PluginProperty("paymentKey", paymentKey, false)
        );

        tossPaymentPluginApi.purchasePayment(
                account.getId(),
                kbPaymentId,
                kbTransactionId,
                account.getPaymentMethodId(),
                BigDecimal.valueOf(10000),
                Currency.KRW,
                properties,
                context
        );

        final var dbRecord = dao.getResponseByPaymentId(kbPaymentId, context.getTenantId());
        Assert.assertNotNull(dbRecord);
        Assert.assertEquals(dbRecord.getPaymentKey(), paymentKey);
    }

    @Test(groups = "slow")
    public void testGetPaymentInfo_NoTransactions_ReturnsEmptyList() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();

        final List<PaymentTransactionInfoPlugin> result = tossPaymentPluginApi.getPaymentInfo(
                account.getId(),
                kbPaymentId,
                Collections.emptyList(),
                context
        );

        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
        Mockito.verify(tossClient, Mockito.never()).getPayment(Mockito.anyString(), Mockito.anyString());
    }

    @Test(groups = "slow")
    public void testGetPaymentInfo_FinalStatus_NoTossApiCall() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();
        final String paymentKey = "test_payment_key_final";
        final String orderId = kbPaymentId.toString();

        final TossPayment mockPayment = createMockTossPayment(paymentKey, orderId, 10000L, "DONE");
        Mockito.when(tossClient.confirmPayment(Mockito.anyString(), Mockito.any(), Mockito.anyString()))
               .thenReturn(mockPayment);

        final List<PluginProperty> properties = ImmutableList.of(
                new PluginProperty("paymentKey", paymentKey, false)
        );

        tossPaymentPluginApi.purchasePayment(
                account.getId(),
                kbPaymentId,
                kbTransactionId,
                account.getPaymentMethodId(),
                BigDecimal.valueOf(10000),
                Currency.KRW,
                properties,
                context
        );

        Mockito.reset(tossClient);

        final List<PaymentTransactionInfoPlugin> result = tossPaymentPluginApi.getPaymentInfo(
                account.getId(),
                kbPaymentId,
                Collections.emptyList(),
                context
        );

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(result.get(0).getStatus(), PaymentPluginStatus.PROCESSED);
        Mockito.verify(tossClient, Mockito.never()).getPayment(Mockito.anyString(), Mockito.anyString());
    }

    @Test(groups = "slow")
    public void testGetPaymentInfo_PendingStatus_SyncsWithToss() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();
        final String paymentKey = "test_payment_key_pending";
        final String orderId = kbPaymentId.toString();

        Mockito.when(tossClient.confirmPayment(Mockito.anyString(), Mockito.any(), Mockito.anyString()))
               .thenThrow(new IOException("Connection timeout"));

        final List<PluginProperty> properties = ImmutableList.of(
                new PluginProperty("paymentKey", paymentKey, false)
        );

        tossPaymentPluginApi.purchasePayment(
                account.getId(),
                kbPaymentId,
                kbTransactionId,
                account.getPaymentMethodId(),
                BigDecimal.valueOf(10000),
                Currency.KRW,
                properties,
                context
        );

        Mockito.reset(tossClient);
        final TossPayment updatedPayment = createMockTossPayment(paymentKey, orderId, 10000L, "DONE");
        Mockito.when(tossClient.getPayment(Mockito.anyString(), Mockito.eq(paymentKey)))
               .thenReturn(updatedPayment);

        final List<PaymentTransactionInfoPlugin> result = tossPaymentPluginApi.getPaymentInfo(
                account.getId(),
                kbPaymentId,
                Collections.emptyList(),
                context
        );

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(result.get(result.size() - 1).getStatus(), PaymentPluginStatus.PROCESSED);
        Mockito.verify(tossClient).getPayment(Mockito.anyString(), Mockito.eq(paymentKey));
    }

    @Test(groups = "slow")
    public void testGetPaymentInfo_TossApiError404_ReturnsERROR() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();
        final String paymentKey = "test_payment_key_api_error";

        Mockito.when(tossClient.confirmPayment(Mockito.anyString(), Mockito.any(), Mockito.anyString()))
               .thenThrow(new IOException("Connection timeout"));

        final List<PluginProperty> properties = ImmutableList.of(
                new PluginProperty("paymentKey", paymentKey, false)
        );

        tossPaymentPluginApi.purchasePayment(
                account.getId(),
                kbPaymentId,
                kbTransactionId,
                account.getPaymentMethodId(),
                BigDecimal.valueOf(10000),
                Currency.KRW,
                properties,
                context
        );

        Mockito.reset(tossClient);
        final TossError tossError = new TossError("NOT_FOUND_PAYMENT", "Payment not found");
        Mockito.when(tossClient.getPayment(Mockito.anyString(), Mockito.eq(paymentKey)))
               .thenThrow(new TossApplicationException(tossError, 404));

        final List<PaymentTransactionInfoPlugin> result = tossPaymentPluginApi.getPaymentInfo(
                account.getId(),
                kbPaymentId,
                Collections.emptyList(),
                context
        );

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(result.get(result.size() - 1).getStatus(), PaymentPluginStatus.ERROR);
        Assert.assertEquals(result.get(result.size() - 1).getGatewayErrorCode(), "NOT_FOUND_PAYMENT");
    }

    @Test(groups = "slow")
    public void testGetPaymentInfo_NetworkError_ReturnsExistingTransactions() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();
        final String paymentKey = "test_payment_key_network";

        Mockito.when(tossClient.confirmPayment(Mockito.anyString(), Mockito.any(), Mockito.anyString()))
               .thenThrow(new IOException("Connection timeout"));

        final List<PluginProperty> properties = ImmutableList.of(
                new PluginProperty("paymentKey", paymentKey, false)
        );

        tossPaymentPluginApi.purchasePayment(
                account.getId(),
                kbPaymentId,
                kbTransactionId,
                account.getPaymentMethodId(),
                BigDecimal.valueOf(10000),
                Currency.KRW,
                properties,
                context
        );

        Mockito.reset(tossClient);
        Mockito.when(tossClient.getPayment(Mockito.anyString(), Mockito.eq(paymentKey)))
               .thenThrow(new IOException("Network unreachable"));

        final List<PaymentTransactionInfoPlugin> result = tossPaymentPluginApi.getPaymentInfo(
                account.getId(),
                kbPaymentId,
                Collections.emptyList(),
                context
        );

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(result.get(result.size() - 1).getStatus(), PaymentPluginStatus.PENDING);
    }

    private TossPayment createMockTossPayment(final String paymentKey, final String orderId, 
                                               final Long totalAmount, final String status) {
        final TossPayment payment = Mockito.mock(TossPayment.class);
        Mockito.when(payment.getPaymentKey()).thenReturn(paymentKey);
        Mockito.when(payment.getOrderId()).thenReturn(orderId);
        Mockito.when(payment.getTotalAmount()).thenReturn(totalAmount);
        Mockito.when(payment.getStatus()).thenReturn(status);
        Mockito.when(payment.getCurrency()).thenReturn("KRW");
        Mockito.when(payment.getMethod()).thenReturn("CARD");
        return payment;
    }

    private TossPayment createMockCanceledTossPayment(final String paymentKey, final String orderId,
                                                       final Long totalAmount, final String status,
                                                       final Long cancelAmount, final String cancelTransactionKey) {
        final TossPayment payment = createMockTossPayment(paymentKey, orderId, totalAmount, status);
        
        final TossPayment.TossCancel mockCancel = Mockito.mock(TossPayment.TossCancel.class);
        Mockito.when(mockCancel.getCancelAmount()).thenReturn(cancelAmount);
        Mockito.when(mockCancel.getTransactionKey()).thenReturn(cancelTransactionKey);
        Mockito.when(mockCancel.getCancelReason()).thenReturn("고객 요청에 의한 환불");
        
        Mockito.when(payment.getCancels()).thenReturn(Collections.singletonList(mockCancel));
        
        return payment;
    }

    @Test(groups = "slow")
    public void testRefundPayment_FullRefund_Success() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbPurchaseTransactionId = UUID.randomUUID();
        final UUID kbRefundTransactionId = UUID.randomUUID();
        final String paymentKey = "test_payment_key_full_refund";
        final String orderId = kbPaymentId.toString();
        final BigDecimal amount = BigDecimal.valueOf(10000);
        final String cancelTransactionKey = "cancel_txn_key_001";

        final TossPayment mockPurchasePayment = createMockTossPayment(paymentKey, orderId, 10000L, "DONE");
        Mockito.when(tossClient.confirmPayment(Mockito.anyString(), Mockito.any(), Mockito.anyString()))
               .thenReturn(mockPurchasePayment);

        final List<PluginProperty> purchaseProperties = ImmutableList.of(
                new PluginProperty("paymentKey", paymentKey, false)
        );

        tossPaymentPluginApi.purchasePayment(
                account.getId(),
                kbPaymentId,
                kbPurchaseTransactionId,
                account.getPaymentMethodId(),
                amount,
                Currency.KRW,
                purchaseProperties,
                context
        );

        final TossPayment mockCanceledPayment = createMockCanceledTossPayment(
                paymentKey, orderId, 10000L, "CANCELED", 10000L, cancelTransactionKey);
        Mockito.when(tossClient.cancelPayment(Mockito.anyString(), Mockito.eq(paymentKey), Mockito.any(), Mockito.anyString()))
               .thenReturn(mockCanceledPayment);

        final PaymentTransactionInfoPlugin result = tossPaymentPluginApi.refundPayment(
                account.getId(),
                kbPaymentId,
                kbRefundTransactionId,
                account.getPaymentMethodId(),
                null,
                Currency.KRW,
                Collections.emptyList(),
                context
        );

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getStatus(), PaymentPluginStatus.PROCESSED);
        Assert.assertEquals(result.getFirstPaymentReferenceId(), paymentKey);
        Assert.assertEquals(result.getSecondPaymentReferenceId(), cancelTransactionKey);
        Assert.assertEquals(result.getKbPaymentId(), kbPaymentId);
        Assert.assertEquals(result.getKbTransactionPaymentId(), kbRefundTransactionId);
    }

    @Test(groups = "slow")
    public void testRefundPayment_PartialRefund_Success() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbPurchaseTransactionId = UUID.randomUUID();
        final UUID kbRefundTransactionId = UUID.randomUUID();
        final String paymentKey = "test_payment_key_partial_refund";
        final String orderId = kbPaymentId.toString();
        final BigDecimal purchaseAmount = BigDecimal.valueOf(10000);
        final BigDecimal refundAmount = BigDecimal.valueOf(3000);
        final String cancelTransactionKey = "cancel_txn_key_002";

        final TossPayment mockPurchasePayment = createMockTossPayment(paymentKey, orderId, 10000L, "DONE");
        Mockito.when(tossClient.confirmPayment(Mockito.anyString(), Mockito.any(), Mockito.anyString()))
               .thenReturn(mockPurchasePayment);

        final List<PluginProperty> purchaseProperties = ImmutableList.of(
                new PluginProperty("paymentKey", paymentKey, false)
        );

        tossPaymentPluginApi.purchasePayment(
                account.getId(),
                kbPaymentId,
                kbPurchaseTransactionId,
                account.getPaymentMethodId(),
                purchaseAmount,
                Currency.KRW,
                purchaseProperties,
                context
        );

        final TossPayment mockCanceledPayment = createMockCanceledTossPayment(
                paymentKey, orderId, 10000L, "PARTIAL_CANCELED", 3000L, cancelTransactionKey);
        Mockito.when(tossClient.cancelPayment(Mockito.anyString(), Mockito.eq(paymentKey), Mockito.any(), Mockito.anyString()))
               .thenReturn(mockCanceledPayment);

        final PaymentTransactionInfoPlugin result = tossPaymentPluginApi.refundPayment(
                account.getId(),
                kbPaymentId,
                kbRefundTransactionId,
                account.getPaymentMethodId(),
                refundAmount,
                Currency.KRW,
                Collections.emptyList(),
                context
        );

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getStatus(), PaymentPluginStatus.PROCESSED);
        Assert.assertEquals(result.getFirstPaymentReferenceId(), paymentKey);
        Assert.assertEquals(result.getSecondPaymentReferenceId(), cancelTransactionKey);
        Assert.assertEquals(result.getAmount().longValue(), refundAmount.longValue());
    }

    @Test(groups = "slow")
    public void testRefundPayment_AlreadyCanceled_ReturnsERROR() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbPurchaseTransactionId = UUID.randomUUID();
        final UUID kbRefundTransactionId = UUID.randomUUID();
        final String paymentKey = "test_payment_key_already_canceled";
        final String orderId = kbPaymentId.toString();
        final BigDecimal amount = BigDecimal.valueOf(10000);

        final TossPayment mockPurchasePayment = createMockTossPayment(paymentKey, orderId, 10000L, "DONE");
        Mockito.when(tossClient.confirmPayment(Mockito.anyString(), Mockito.any(), Mockito.anyString()))
               .thenReturn(mockPurchasePayment);

        final List<PluginProperty> purchaseProperties = ImmutableList.of(
                new PluginProperty("paymentKey", paymentKey, false)
        );

        tossPaymentPluginApi.purchasePayment(
                account.getId(),
                kbPaymentId,
                kbPurchaseTransactionId,
                account.getPaymentMethodId(),
                amount,
                Currency.KRW,
                purchaseProperties,
                context
        );

        final TossError tossError = new TossError("ALREADY_CANCELED_PAYMENT", "이미 취소된 결제입니다.");
        final TossApplicationException exception = new TossApplicationException(tossError, 400);
        Mockito.when(tossClient.cancelPayment(Mockito.anyString(), Mockito.eq(paymentKey), Mockito.any(), Mockito.anyString()))
               .thenThrow(exception);

        final PaymentTransactionInfoPlugin result = tossPaymentPluginApi.refundPayment(
                account.getId(),
                kbPaymentId,
                kbRefundTransactionId,
                account.getPaymentMethodId(),
                amount,
                Currency.KRW,
                Collections.emptyList(),
                context
        );

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getStatus(), PaymentPluginStatus.ERROR);
        Assert.assertEquals(result.getGatewayErrorCode(), "ALREADY_CANCELED_PAYMENT");
        Assert.assertNotNull(result.getGatewayError());
    }

    @Test(groups = "slow")
    public void testRefundPayment_ExceedMaxRefundAmount_ReturnsERROR() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbPurchaseTransactionId = UUID.randomUUID();
        final UUID kbRefundTransactionId = UUID.randomUUID();
        final String paymentKey = "test_payment_key_exceed_amount";
        final String orderId = kbPaymentId.toString();
        final BigDecimal purchaseAmount = BigDecimal.valueOf(10000);
        final BigDecimal refundAmount = BigDecimal.valueOf(5000);

        final TossPayment mockPurchasePayment = createMockTossPayment(paymentKey, orderId, 10000L, "DONE");
        Mockito.when(tossClient.confirmPayment(Mockito.anyString(), Mockito.any(), Mockito.anyString()))
               .thenReturn(mockPurchasePayment);

        final List<PluginProperty> purchaseProperties = ImmutableList.of(
                new PluginProperty("paymentKey", paymentKey, false)
        );

        tossPaymentPluginApi.purchasePayment(
                account.getId(),
                kbPaymentId,
                kbPurchaseTransactionId,
                account.getPaymentMethodId(),
                purchaseAmount,
                Currency.KRW,
                purchaseProperties,
                context
        );

        final TossError tossError = new TossError("EXCEED_MAX_REFUND_AMOUNT", "환불 가능한 금액을 초과했습니다.");
        final TossApplicationException exception = new TossApplicationException(tossError, 400);
        Mockito.when(tossClient.cancelPayment(Mockito.anyString(), Mockito.eq(paymentKey), Mockito.any(), Mockito.anyString()))
               .thenThrow(exception);

        final PaymentTransactionInfoPlugin result = tossPaymentPluginApi.refundPayment(
                account.getId(),
                kbPaymentId,
                kbRefundTransactionId,
                account.getPaymentMethodId(),
                refundAmount,
                Currency.KRW,
                Collections.emptyList(),
                context
        );

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getStatus(), PaymentPluginStatus.ERROR);
        Assert.assertEquals(result.getGatewayErrorCode(), "EXCEED_MAX_REFUND_AMOUNT");
    }

    @Test(groups = "slow")
    public void testRefundPayment_NetworkError_ReturnsPENDING() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbPurchaseTransactionId = UUID.randomUUID();
        final UUID kbRefundTransactionId = UUID.randomUUID();
        final String paymentKey = "test_payment_key_network_error_refund";
        final String orderId = kbPaymentId.toString();
        final BigDecimal amount = BigDecimal.valueOf(10000);

        final TossPayment mockPurchasePayment = createMockTossPayment(paymentKey, orderId, 10000L, "DONE");
        Mockito.when(tossClient.confirmPayment(Mockito.anyString(), Mockito.any(), Mockito.anyString()))
               .thenReturn(mockPurchasePayment);

        final List<PluginProperty> purchaseProperties = ImmutableList.of(
                new PluginProperty("paymentKey", paymentKey, false)
        );

        tossPaymentPluginApi.purchasePayment(
                account.getId(),
                kbPaymentId,
                kbPurchaseTransactionId,
                account.getPaymentMethodId(),
                amount,
                Currency.KRW,
                purchaseProperties,
                context
        );

        Mockito.when(tossClient.cancelPayment(Mockito.anyString(), Mockito.eq(paymentKey), Mockito.any(), Mockito.anyString()))
               .thenThrow(new IOException("Connection timeout"));

        final PaymentTransactionInfoPlugin result = tossPaymentPluginApi.refundPayment(
                account.getId(),
                kbPaymentId,
                kbRefundTransactionId,
                account.getPaymentMethodId(),
                amount,
                Currency.KRW,
                Collections.emptyList(),
                context
        );

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getStatus(), PaymentPluginStatus.PENDING);
        Assert.assertEquals(result.getGatewayErrorCode(), "NETWORK_ERROR");
        Assert.assertEquals(result.getFirstPaymentReferenceId(), paymentKey);
    }

    @Test(groups = "slow", expectedExceptions = PaymentPluginApiException.class)
    public void testRefundPayment_OriginalPaymentNotFound_ThrowsException() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbRefundTransactionId = UUID.randomUUID();

        tossPaymentPluginApi.refundPayment(
                account.getId(),
                kbPaymentId,
                kbRefundTransactionId,
                account.getPaymentMethodId(),
                BigDecimal.valueOf(10000),
                Currency.KRW,
                Collections.emptyList(),
                context
        );
    }

    @Test(groups = "slow")
    public void testRefundPayment_SavesToDB() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbPurchaseTransactionId = UUID.randomUUID();
        final UUID kbRefundTransactionId = UUID.randomUUID();
        final String paymentKey = "test_payment_key_refund_db";
        final String orderId = kbPaymentId.toString();
        final BigDecimal amount = BigDecimal.valueOf(10000);
        final String cancelTransactionKey = "cancel_txn_key_db";

        // 1. Create successful purchase
        final TossPayment mockPurchasePayment = createMockTossPayment(paymentKey, orderId, 10000L, "DONE");
        Mockito.when(tossClient.confirmPayment(Mockito.anyString(), Mockito.any(), Mockito.anyString()))
               .thenReturn(mockPurchasePayment);

        final List<PluginProperty> purchaseProperties = ImmutableList.of(
                new PluginProperty("paymentKey", paymentKey, false)
        );

        tossPaymentPluginApi.purchasePayment(
                account.getId(),
                kbPaymentId,
                kbPurchaseTransactionId,
                account.getPaymentMethodId(),
                amount,
                Currency.KRW,
                purchaseProperties,
                context
        );

        // 2. Mock cancel API
        final TossPayment mockCanceledPayment = createMockCanceledTossPayment(
                paymentKey, orderId, 10000L, "CANCELED", 10000L, cancelTransactionKey);
        Mockito.when(tossClient.cancelPayment(Mockito.anyString(), Mockito.eq(paymentKey), Mockito.any(), Mockito.anyString()))
               .thenReturn(mockCanceledPayment);

        // 3. Call refundPayment
        tossPaymentPluginApi.refundPayment(
                account.getId(),
                kbPaymentId,
                kbRefundTransactionId,
                account.getPaymentMethodId(),
                amount,
                Currency.KRW,
                Collections.emptyList(),
                context
        );

        // 4. Verify DB record - should have REFUND transaction type
        final var dbRecord = dao.getResponseByPaymentId(kbPaymentId, context.getTenantId());
        Assert.assertNotNull(dbRecord);
        Assert.assertEquals(dbRecord.getTransactionType(), "REFUND");
        Assert.assertEquals(dbRecord.getPaymentKey(), paymentKey);
        Assert.assertEquals(dbRecord.getTossPaymentStatus(), "CANCELED");
    }

    @Test(groups = "slow")
    public void testGetPaymentInfo_AuthError_ReturnsERROR() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();
        final String paymentKey = "test_payment_key_auth_error";

        Mockito.when(tossClient.confirmPayment(Mockito.anyString(), Mockito.any(), Mockito.anyString()))
               .thenThrow(new IOException("Connection timeout"));

        final List<PluginProperty> properties = ImmutableList.of(
                new PluginProperty("paymentKey", paymentKey, false)
        );

        tossPaymentPluginApi.purchasePayment(
                account.getId(),
                kbPaymentId,
                kbTransactionId,
                account.getPaymentMethodId(),
                BigDecimal.valueOf(10000),
                Currency.KRW,
                properties,
                context
        );

        Mockito.reset(tossClient);
        final TossError tossError = new TossError("UNAUTHORIZED_KEY", "Invalid secret key");
        Mockito.when(tossClient.getPayment(Mockito.anyString(), Mockito.eq(paymentKey)))
               .thenThrow(new TossApplicationException(tossError, 401));

        final List<PaymentTransactionInfoPlugin> result = tossPaymentPluginApi.getPaymentInfo(
                account.getId(),
                kbPaymentId,
                Collections.emptyList(),
                context
        );

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(result.get(result.size() - 1).getStatus(), PaymentPluginStatus.ERROR);
        Assert.assertEquals(result.get(result.size() - 1).getGatewayErrorCode(), "UNAUTHORIZED_KEY");
    }

    @Test(groups = "slow")
    public void testGetPaymentInfo_5xxError_ReturnsPENDING() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();
        final String paymentKey = "test_payment_key_5xx_error";

        Mockito.when(tossClient.confirmPayment(Mockito.anyString(), Mockito.any(), Mockito.anyString()))
               .thenThrow(new IOException("Connection timeout"));

        final List<PluginProperty> properties = ImmutableList.of(
                new PluginProperty("paymentKey", paymentKey, false)
        );

        tossPaymentPluginApi.purchasePayment(
                account.getId(),
                kbPaymentId,
                kbTransactionId,
                account.getPaymentMethodId(),
                BigDecimal.valueOf(10000),
                Currency.KRW,
                properties,
                context
        );

        Mockito.reset(tossClient);
        final TossError tossError = new TossError("FAILED_INTERNAL_SYSTEM_PROCESSING", "Internal error");
        Mockito.when(tossClient.getPayment(Mockito.anyString(), Mockito.eq(paymentKey)))
               .thenThrow(new TossApplicationException(tossError, 500));

        final List<PaymentTransactionInfoPlugin> result = tossPaymentPluginApi.getPaymentInfo(
                account.getId(),
                kbPaymentId,
                Collections.emptyList(),
                context
        );

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(result.get(result.size() - 1).getStatus(), PaymentPluginStatus.PENDING);
    }

    @Test(groups = "slow")
    public void testGetPaymentInfo_SyncsWithToss_ExpiredStatus_ReturnsCanceled() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();
        final String paymentKey = "test_payment_key_expired";
        final String orderId = kbPaymentId.toString();

        Mockito.when(tossClient.confirmPayment(Mockito.anyString(), Mockito.any(), Mockito.anyString()))
               .thenThrow(new IOException("Connection timeout"));

        final List<PluginProperty> properties = ImmutableList.of(
                new PluginProperty("paymentKey", paymentKey, false)
        );

        tossPaymentPluginApi.purchasePayment(
                account.getId(),
                kbPaymentId,
                kbTransactionId,
                account.getPaymentMethodId(),
                BigDecimal.valueOf(10000),
                Currency.KRW,
                properties,
                context
        );

        Mockito.reset(tossClient);
        final TossPayment expiredPayment = createMockTossPayment(paymentKey, orderId, 10000L, "EXPIRED");
        Mockito.when(tossClient.getPayment(Mockito.anyString(), Mockito.eq(paymentKey)))
               .thenReturn(expiredPayment);

        final List<PaymentTransactionInfoPlugin> result = tossPaymentPluginApi.getPaymentInfo(
                account.getId(),
                kbPaymentId,
                Collections.emptyList(),
                context
        );

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(result.get(result.size() - 1).getStatus(), PaymentPluginStatus.CANCELED);
    }

    @Test(groups = "slow")
    public void testGetPaymentInfo_SyncsWithToss_CanceledStatus_ReturnsProcessed() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();
        final String paymentKey = "test_payment_key_canceled";
        final String orderId = kbPaymentId.toString();

        Mockito.when(tossClient.confirmPayment(Mockito.anyString(), Mockito.any(), Mockito.anyString()))
               .thenThrow(new IOException("Connection timeout"));

        final List<PluginProperty> properties = ImmutableList.of(
                new PluginProperty("paymentKey", paymentKey, false)
        );

        tossPaymentPluginApi.purchasePayment(
                account.getId(),
                kbPaymentId,
                kbTransactionId,
                account.getPaymentMethodId(),
                BigDecimal.valueOf(10000),
                Currency.KRW,
                properties,
                context
        );

        Mockito.reset(tossClient);
        final TossPayment canceledPayment = createMockTossPayment(paymentKey, orderId, 10000L, "CANCELED");
        Mockito.when(tossClient.getPayment(Mockito.anyString(), Mockito.eq(paymentKey)))
               .thenReturn(canceledPayment);

        final List<PaymentTransactionInfoPlugin> result = tossPaymentPluginApi.getPaymentInfo(
                account.getId(),
                kbPaymentId,
                Collections.emptyList(),
                context
        );

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(result.get(result.size() - 1).getStatus(), PaymentPluginStatus.PROCESSED);
    }

    @Test(groups = "slow")
    public void testGetPaymentInfo_RefundPending_TossDone_KeepsPending() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbPurchaseTransactionId = UUID.randomUUID();
        final UUID kbRefundTransactionId = UUID.randomUUID();
        final String paymentKey = "test_payment_key_refund_done";
        final String orderId = kbPaymentId.toString();
        final BigDecimal amount = BigDecimal.valueOf(10000);

        final TossPayment mockPurchasePayment = createMockTossPayment(paymentKey, orderId, 10000L, "DONE");
        Mockito.when(tossClient.confirmPayment(Mockito.anyString(), Mockito.any(), Mockito.anyString()))
               .thenReturn(mockPurchasePayment);

        final List<PluginProperty> purchaseProperties = ImmutableList.of(
                new PluginProperty("paymentKey", paymentKey, false)
        );

        tossPaymentPluginApi.purchasePayment(
                account.getId(),
                kbPaymentId,
                kbPurchaseTransactionId,
                account.getPaymentMethodId(),
                amount,
                Currency.KRW,
                purchaseProperties,
                context
        );

        Mockito.when(tossClient.cancelPayment(Mockito.anyString(), Mockito.eq(paymentKey), Mockito.any(), Mockito.anyString()))
               .thenThrow(new IOException("Network timeout"));

        tossPaymentPluginApi.refundPayment(
                account.getId(),
                kbPaymentId,
                kbRefundTransactionId,
                account.getPaymentMethodId(),
                amount,
                Currency.KRW,
                Collections.emptyList(),
                context
        );

        Mockito.reset(tossClient);
        final TossPayment stillDonePayment = createMockTossPayment(paymentKey, orderId, 10000L, "DONE");
        Mockito.when(tossClient.getPayment(Mockito.anyString(), Mockito.eq(paymentKey)))
               .thenReturn(stillDonePayment);

        final List<PaymentTransactionInfoPlugin> result = tossPaymentPluginApi.getPaymentInfo(
                account.getId(),
                kbPaymentId,
                Collections.emptyList(),
                context
        );

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        final PaymentTransactionInfoPlugin lastTx = result.get(result.size() - 1);
        Assert.assertEquals(lastTx.getStatus(), PaymentPluginStatus.PENDING);
    }

    @Test(groups = "slow")
    public void testGetPaymentInfo_RefundPending_TossCanceled_ReturnsProcessed() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbPurchaseTransactionId = UUID.randomUUID();
        final UUID kbRefundTransactionId = UUID.randomUUID();
        final String paymentKey = "test_payment_key_refund_canceled";
        final String orderId = kbPaymentId.toString();
        final BigDecimal amount = BigDecimal.valueOf(10000);

        final TossPayment mockPurchasePayment = createMockTossPayment(paymentKey, orderId, 10000L, "DONE");
        Mockito.when(tossClient.confirmPayment(Mockito.anyString(), Mockito.any(), Mockito.anyString()))
               .thenReturn(mockPurchasePayment);

        final List<PluginProperty> purchaseProperties = ImmutableList.of(
                new PluginProperty("paymentKey", paymentKey, false)
        );

        tossPaymentPluginApi.purchasePayment(
                account.getId(),
                kbPaymentId,
                kbPurchaseTransactionId,
                account.getPaymentMethodId(),
                amount,
                Currency.KRW,
                purchaseProperties,
                context
        );

        Mockito.when(tossClient.cancelPayment(Mockito.anyString(), Mockito.eq(paymentKey), Mockito.any(), Mockito.anyString()))
               .thenThrow(new IOException("Network timeout"));

        tossPaymentPluginApi.refundPayment(
                account.getId(),
                kbPaymentId,
                kbRefundTransactionId,
                account.getPaymentMethodId(),
                amount,
                Currency.KRW,
                Collections.emptyList(),
                context
        );

        Mockito.reset(tossClient);
        final TossPayment canceledPayment = createMockTossPayment(paymentKey, orderId, 10000L, "CANCELED");
        Mockito.when(tossClient.getPayment(Mockito.anyString(), Mockito.eq(paymentKey)))
               .thenReturn(canceledPayment);

        final List<PaymentTransactionInfoPlugin> result = tossPaymentPluginApi.getPaymentInfo(
                account.getId(),
                kbPaymentId,
                Collections.emptyList(),
                context
        );

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        final PaymentTransactionInfoPlugin lastTx = result.get(result.size() - 1);
        Assert.assertEquals(lastTx.getStatus(), PaymentPluginStatus.PROCESSED);
    }

    @Test(groups = "slow")
    public void testPurchaseWithAuthKeyAndStorePaymentMethod_Success() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();
        final String authKey = "test_auth_key_123";
        final String billingKey = "billing_key_abc";
        final String customerKey = account.getPaymentMethodId().toString();
        final String paymentKey = "payment_key_xyz";
        final String orderId = kbPaymentId.toString();
        final BigDecimal amount = BigDecimal.valueOf(10000);

        final TossBilling mockBilling = createMockTossBilling(billingKey, customerKey);
        Mockito.when(tossClient.issueBillingKey(Mockito.anyString(), Mockito.any()))
               .thenReturn(mockBilling);

        final TossPayment mockPayment = createMockTossPayment(paymentKey, orderId, 10000L, "DONE");
        Mockito.when(tossClient.executeBillingKeyPayment(Mockito.anyString(), Mockito.eq(billingKey), Mockito.any(), Mockito.anyString()))
               .thenReturn(mockPayment);

        final List<PluginProperty> properties = ImmutableList.of(
                new PluginProperty("authKey", authKey, false),
                new PluginProperty("storePaymentMethod", "true", false)
        );

        final PaymentTransactionInfoPlugin result = tossPaymentPluginApi.purchasePayment(
                account.getId(),
                kbPaymentId,
                kbTransactionId,
                account.getPaymentMethodId(),
                amount,
                Currency.KRW,
                properties,
                context
        );

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getStatus(), PaymentPluginStatus.PROCESSED);
        Assert.assertEquals(result.getFirstPaymentReferenceId(), paymentKey);

        Mockito.verify(tossClient).issueBillingKey(Mockito.anyString(), Mockito.any());
        Mockito.verify(tossClient).executeBillingKeyPayment(Mockito.anyString(), Mockito.eq(billingKey), Mockito.any(), Mockito.anyString());

        final var paymentMethod = dao.getPaymentMethod(account.getPaymentMethodId(), context.getTenantId());
        Assert.assertNotNull(paymentMethod);
        Assert.assertEquals(paymentMethod.getBillingKey(), billingKey);
        Assert.assertEquals(paymentMethod.getCustomerKey(), customerKey);
    }

    @Test(groups = "slow")
    public void testPurchaseWithAuthKeyAndStorePaymentMethod_BillingKeyIssueFails_ReturnsError() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();
        final String authKey = "test_auth_key_fail";
        final BigDecimal amount = BigDecimal.valueOf(10000);

        final TossError tossError = new TossError("INVALID_AUTH_KEY", "Invalid auth key");
        final TossApplicationException exception = new TossApplicationException(tossError, 400);
        Mockito.when(tossClient.issueBillingKey(Mockito.anyString(), Mockito.any()))
               .thenThrow(exception);

        final List<PluginProperty> properties = ImmutableList.of(
                new PluginProperty("authKey", authKey, false),
                new PluginProperty("storePaymentMethod", "true", false)
        );

        final PaymentTransactionInfoPlugin result = tossPaymentPluginApi.purchasePayment(
                account.getId(),
                kbPaymentId,
                kbTransactionId,
                account.getPaymentMethodId(),
                amount,
                Currency.KRW,
                properties,
                context
        );

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getStatus(), PaymentPluginStatus.ERROR);
        Assert.assertEquals(result.getGatewayErrorCode(), "INVALID_AUTH_KEY");

        Mockito.verify(tossClient).issueBillingKey(Mockito.anyString(), Mockito.any());
        Mockito.verify(tossClient, Mockito.never()).executeBillingKeyPayment(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.anyString());
    }

    @Test(groups = "slow")
    public void testPurchaseWithAuthKeyAndStorePaymentMethod_BillingKeySuccess_PaymentFails() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();
        final String authKey = "test_auth_key_payment_fail";
        final String billingKey = "billing_key_payment_fail";
        final String customerKey = account.getPaymentMethodId().toString();
        final BigDecimal amount = BigDecimal.valueOf(10000);

        final TossBilling mockBilling = createMockTossBilling(billingKey, customerKey);
        Mockito.when(tossClient.issueBillingKey(Mockito.anyString(), Mockito.any()))
               .thenReturn(mockBilling);

        final TossError tossError = new TossError("EXCEED_MAX_CARD_INSTALLMENT_PLAN", "Exceeds card limit");
        final TossApplicationException exception = new TossApplicationException(tossError, 400);
        Mockito.when(tossClient.executeBillingKeyPayment(Mockito.anyString(), Mockito.eq(billingKey), Mockito.any(), Mockito.anyString()))
               .thenThrow(exception);

        final List<PluginProperty> properties = ImmutableList.of(
                new PluginProperty("authKey", authKey, false),
                new PluginProperty("storePaymentMethod", "true", false)
        );

        final PaymentTransactionInfoPlugin result = tossPaymentPluginApi.purchasePayment(
                account.getId(),
                kbPaymentId,
                kbTransactionId,
                account.getPaymentMethodId(),
                amount,
                Currency.KRW,
                properties,
                context
        );

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getStatus(), PaymentPluginStatus.ERROR);
        Assert.assertEquals(result.getGatewayErrorCode(), "EXCEED_MAX_CARD_INSTALLMENT_PLAN");

        final var paymentMethod = dao.getPaymentMethod(account.getPaymentMethodId(), context.getTenantId());
        Assert.assertNotNull(paymentMethod, "Billing key should be saved even if payment fails");
        Assert.assertEquals(paymentMethod.getBillingKey(), billingKey);
    }

    @Test(groups = "slow")
    public void testPurchaseWithStoredBillingKey_Success() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();
        final String billingKey = "stored_billing_key_001";
        final String customerKey = account.getPaymentMethodId().toString();
        final String paymentKey = "payment_key_stored";
        final String orderId = kbPaymentId.toString();
        final BigDecimal amount = BigDecimal.valueOf(5000);

        final TossBilling mockBilling = createMockTossBilling(billingKey, customerKey);
        dao.addPaymentMethod(account.getId(), account.getPaymentMethodId(), true, mockBilling, clock.getUTCNow(), context.getTenantId());

        final TossPayment mockPayment = createMockTossPayment(paymentKey, orderId, 5000L, "DONE");
        Mockito.when(tossClient.executeBillingKeyPayment(Mockito.anyString(), Mockito.eq(billingKey), Mockito.any(), Mockito.anyString()))
               .thenReturn(mockPayment);

        final PaymentTransactionInfoPlugin result = tossPaymentPluginApi.purchasePayment(
                account.getId(),
                kbPaymentId,
                kbTransactionId,
                account.getPaymentMethodId(),
                amount,
                Currency.KRW,
                Collections.emptyList(),
                context
        );

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getStatus(), PaymentPluginStatus.PROCESSED);
        Assert.assertEquals(result.getFirstPaymentReferenceId(), paymentKey);

        Mockito.verify(tossClient, Mockito.never()).issueBillingKey(Mockito.anyString(), Mockito.any());
        Mockito.verify(tossClient).executeBillingKeyPayment(Mockito.anyString(), Mockito.eq(billingKey), Mockito.any(), Mockito.anyString());
    }

    @Test(groups = "slow", expectedExceptions = PaymentPluginApiException.class)
    public void testPurchaseWithStoredBillingKey_NoBillingKey_ThrowsException() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();
        final BigDecimal amount = BigDecimal.valueOf(5000);

        tossPaymentPluginApi.purchasePayment(
                account.getId(),
                kbPaymentId,
                kbTransactionId,
                account.getPaymentMethodId(),
                amount,
                Currency.KRW,
                Collections.emptyList(),
                context
        );
    }

    @Test(groups = "slow")
    public void testPurchaseWithStoredBillingKey_PaymentFails_ReturnsError() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();
        final String billingKey = "stored_billing_key_fail";
        final String customerKey = account.getPaymentMethodId().toString();
        final BigDecimal amount = BigDecimal.valueOf(5000);

        final TossBilling mockBilling = createMockTossBilling(billingKey, customerKey);
        dao.addPaymentMethod(account.getId(), account.getPaymentMethodId(), true, mockBilling, clock.getUTCNow(), context.getTenantId());

        final TossError tossError = new TossError("INVALID_CARD_NUMBER", "Card number is invalid");
        final TossApplicationException exception = new TossApplicationException(tossError, 400);
        Mockito.when(tossClient.executeBillingKeyPayment(Mockito.anyString(), Mockito.eq(billingKey), Mockito.any(), Mockito.anyString()))
               .thenThrow(exception);

        final PaymentTransactionInfoPlugin result = tossPaymentPluginApi.purchasePayment(
                account.getId(),
                kbPaymentId,
                kbTransactionId,
                account.getPaymentMethodId(),
                amount,
                Currency.KRW,
                Collections.emptyList(),
                context
        );

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getStatus(), PaymentPluginStatus.ERROR);
        Assert.assertEquals(result.getGatewayErrorCode(), "INVALID_CARD_NUMBER");
    }

    private TossBilling createMockTossBilling(final String billingKey, final String customerKey) {
        final TossBilling billing = Mockito.mock(TossBilling.class);
        Mockito.when(billing.getBillingKey()).thenReturn(billingKey);
        Mockito.when(billing.getCustomerKey()).thenReturn(customerKey);
        Mockito.when(billing.getMethod()).thenReturn("CARD");
        Mockito.when(billing.getCardCompany()).thenReturn("삼성카드");
        Mockito.when(billing.getCardNumber()).thenReturn("****-****-****-1234");
        return billing;
    }
}
