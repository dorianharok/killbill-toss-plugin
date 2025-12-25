package org.killbill.billing.plugin.toss.client;

import org.killbill.billing.plugin.toss.client.exception.TossApplicationException;
import org.killbill.billing.plugin.toss.client.model.PaymentCancelRequest;
import org.killbill.billing.plugin.toss.client.model.PaymentConfirmRequest;
import org.killbill.billing.plugin.toss.client.model.TossPayment;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

public class TossClientTest {

    private static final String TEST_SECRET_KEY = "test_gsk_docs_OaPz8L5KdmQXkzRz3y47BMw6";
    private TossClient tossClient;

    @BeforeMethod
    public void setup() {
        // Use real Toss Payments API
        tossClient = new TossClientImpl();
    }

    @Test(groups = "integration")
    public void testConfirmPayment_InvalidOrder() {
        // We cannot easily create a valid paymentKey without frontend interaction.
        // Instead, we verify that the client communicates with Toss and receives a valid error response.
        
        PaymentConfirmRequest request = new PaymentConfirmRequest("invalid_payment_key", "order_id_123", 1000L);
        
        try {
            tossClient.confirmPayment(TEST_SECRET_KEY, request, "test-idempotency-key");
            Assert.fail("Should throw TossApplicationException");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof TossApplicationException);
            TossApplicationException tae = (TossApplicationException) e;
            // Expecting 404 NOT_FOUND or 400 BAD_REQUEST depending on validation
            // Toss usually returns 404 for invalid paymentKey, or 400 if format is wrong.
            Assert.assertTrue(tae.getStatusCode() >= 400);
            Assert.assertNotNull(tae.getTossError());
            
            // Helpful logging
            System.out.println("Toss Error Code: " + tae.getTossError().getCode());
            System.out.println("Toss Error Message: " + tae.getTossError().getMessage());
        }
    }

    @Test(groups = "integration")
    public void testGetPayment_NotFound() {
        try {
            tossClient.getPayment(TEST_SECRET_KEY, "non_existent_payment_key");
            Assert.fail("Should throw TossApplicationException");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof TossApplicationException);
            TossApplicationException tae = (TossApplicationException) e;
            Assert.assertEquals(tae.getStatusCode(), 404);
            Assert.assertNotNull(tae.getTossError());
            // Toss API returns NOT_FOUND_PAYMENT for unknown paymentKey
            Assert.assertTrue(tae.getTossError().getCode().contains("NOT_FOUND")); 
        }
    }
    
    @Test(groups = "integration")
    public void testCancelPayment_NotFound() {
        final PaymentCancelRequest request = new PaymentCancelRequest("Customer requested cancel", 1000L);
        try {
            tossClient.cancelPayment(TEST_SECRET_KEY, "non_existent_payment_key", request, "test-idempotency-key");
            Assert.fail("Should throw TossApplicationException");
        } catch (Exception e) {
             Assert.assertTrue(e instanceof TossApplicationException);
             TossApplicationException tae = (TossApplicationException) e;
             Assert.assertEquals(tae.getStatusCode(), 404);
        }
    }
}
