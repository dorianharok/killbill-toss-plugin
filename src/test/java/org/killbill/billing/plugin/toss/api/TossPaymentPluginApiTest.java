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
import org.killbill.billing.plugin.toss.client.model.TossError;
import org.killbill.billing.plugin.toss.client.model.TossPayment;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

public class TossPaymentPluginApiTest extends TestBase {

    // ========================================
    // Unsupported Methods Tests (AC: 7)
    // ========================================

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

        // Mock Toss API response
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
                    Collections.emptyList(),  // Missing paymentKey
                    context
            );
            Assert.fail("Should throw PaymentPluginApiException for missing paymentKey");
        } catch (final PaymentPluginApiException e) {
            // Expected - paymentKey is required
            Assert.assertNotNull(e);
        }
    }

    @Test(groups = "slow")
    public void testPurchasePayment_4xxError_ReturnsERROR() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();
        final String paymentKey = "test_payment_key_123";

        // Mock 4xx error from Toss
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

        // Mock 5xx error from Toss
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

        // Mock network error
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
        // paymentKey should be preserved for Janitor recovery
        Assert.assertEquals(result.getFirstPaymentReferenceId(), paymentKey);
    }

    @Test(groups = "slow")
    public void testPurchasePayment_Success_SavesToDB() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();
        final String paymentKey = "test_payment_key_db";
        final String orderId = kbPaymentId.toString();

        // Mock Toss API response
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

        // Verify DB record
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

        // Mock network error
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

        // Verify paymentKey is saved even on network error
        final var dbRecord = dao.getResponseByPaymentId(kbPaymentId, context.getTenantId());
        Assert.assertNotNull(dbRecord, "DB record should exist even on network error");
        Assert.assertEquals(dbRecord.getPaymentKey(), paymentKey, "paymentKey must be saved for Janitor recovery");
    }

    // ========================================
    // getPaymentInfo Tests
    // ========================================

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
        // Verify Toss API was NOT called
        Mockito.verify(tossClient, Mockito.never()).getPayment(Mockito.anyString(), Mockito.anyString());
    }

    @Test(groups = "slow")
    public void testGetPaymentInfo_FinalStatus_NoTossApiCall() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();
        final String paymentKey = "test_payment_key_final";
        final String orderId = kbPaymentId.toString();

        // First, create a successful purchase (status = DONE = PROCESSED)
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

        // Reset mock to track new calls
        Mockito.reset(tossClient);

        // Call getPaymentInfo
        final List<PaymentTransactionInfoPlugin> result = tossPaymentPluginApi.getPaymentInfo(
                account.getId(),
                kbPaymentId,
                Collections.emptyList(),
                context
        );

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(result.get(0).getStatus(), PaymentPluginStatus.PROCESSED);
        // Verify Toss API was NOT called for final status
        Mockito.verify(tossClient, Mockito.never()).getPayment(Mockito.anyString(), Mockito.anyString());
    }

    @Test(groups = "slow")
    public void testGetPaymentInfo_PendingStatus_SyncsWithToss() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();
        final String paymentKey = "test_payment_key_pending";
        final String orderId = kbPaymentId.toString();

        // First, create a pending purchase (network error)
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

        // Reset mock and set up getPayment to return success
        Mockito.reset(tossClient);
        final TossPayment updatedPayment = createMockTossPayment(paymentKey, orderId, 10000L, "DONE");
        Mockito.when(tossClient.getPayment(Mockito.anyString(), Mockito.eq(paymentKey)))
               .thenReturn(updatedPayment);

        // Call getPaymentInfo - should sync with Toss
        final List<PaymentTransactionInfoPlugin> result = tossPaymentPluginApi.getPaymentInfo(
                account.getId(),
                kbPaymentId,
                Collections.emptyList(),
                context
        );

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        // Status should be updated to PROCESSED after sync
        Assert.assertEquals(result.get(result.size() - 1).getStatus(), PaymentPluginStatus.PROCESSED);
        // Verify Toss API was called
        Mockito.verify(tossClient).getPayment(Mockito.anyString(), Mockito.eq(paymentKey));
    }

    @Test(groups = "slow")
    public void testGetPaymentInfo_TossApiError_ReturnsExistingTransactions() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();
        final String paymentKey = "test_payment_key_api_error";

        // Create a pending purchase
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

        // Reset and set up getPayment to throw error
        Mockito.reset(tossClient);
        final TossError tossError = new TossError("NOT_FOUND", "Payment not found");
        Mockito.when(tossClient.getPayment(Mockito.anyString(), Mockito.eq(paymentKey)))
               .thenThrow(new TossApplicationException(tossError, 404));

        // Call getPaymentInfo - should return existing transactions
        final List<PaymentTransactionInfoPlugin> result = tossPaymentPluginApi.getPaymentInfo(
                account.getId(),
                kbPaymentId,
                Collections.emptyList(),
                context
        );

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        // Status should remain PENDING (not updated due to API error)
        Assert.assertEquals(result.get(result.size() - 1).getStatus(), PaymentPluginStatus.PENDING);
    }

    @Test(groups = "slow")
    public void testGetPaymentInfo_NetworkError_ReturnsExistingTransactions() throws Exception {
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();
        final String paymentKey = "test_payment_key_network";

        // Create a pending purchase
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

        // Reset and set up getPayment to throw network error
        Mockito.reset(tossClient);
        Mockito.when(tossClient.getPayment(Mockito.anyString(), Mockito.eq(paymentKey)))
               .thenThrow(new IOException("Network unreachable"));

        // Call getPaymentInfo - should return existing transactions
        final List<PaymentTransactionInfoPlugin> result = tossPaymentPluginApi.getPaymentInfo(
                account.getId(),
                kbPaymentId,
                Collections.emptyList(),
                context
        );

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        // Status should remain PENDING
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
}
