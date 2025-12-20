package org.killbill.billing.plugin.toss.client.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.Assert;
import org.testng.annotations.Test;

public class PaymentConfirmRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testSerialization() throws JsonProcessingException {
        PaymentConfirmRequest request = new PaymentConfirmRequest("request_payment_key", "request_order_id", 1000L);
        String json = objectMapper.writeValueAsString(request);

        Assert.assertTrue(json.contains("\"paymentKey\":\"request_payment_key\""));
        Assert.assertTrue(json.contains("\"orderId\":\"request_order_id\""));
        Assert.assertTrue(json.contains("\"amount\":1000"));
    }
}
