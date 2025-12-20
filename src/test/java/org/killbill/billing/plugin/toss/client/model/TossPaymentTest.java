package org.killbill.billing.plugin.toss.client.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TossPaymentTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testDeserialization() throws JsonProcessingException {
        String json = "{" +
                "  \"mId\": \"tosspayments\"," +
                "  \"lastTransactionKey\": \"9C62B18EEF0DE3EB7F4422EB6D14BC6E\"," +
                "  \"paymentKey\": \"5EnNZRJGvaBX7zk2yd8ydw26XvwXkLrx9POLqKQjmAw4b0e1\"," +
                "  \"orderId\": \"a4CWyWY5m89PNh7xJwhk1\"," +
                "  \"orderName\": \"토스 티셔츠 외 2건\"," +
                "  \"taxExemptionAmount\": 0," +
                "  \"status\": \"DONE\"," +
                "  \"requestedAt\": \"2024-02-13T12:17:57+09:00\"," +
                "  \"approvedAt\": \"2024-02-13T12:18:14+09:00\"," +
                "  \"useEscrow\": false," +
                "  \"cultureExpense\": false," +
                "  \"card\": {" +
                "    \"issuerCode\": \"71\"," +
                "    \"acquirerCode\": \"71\"," +
                "    \"number\": \"12345678****000*\"," +
                "    \"installmentPlanMonths\": 0," +
                "    \"isInterestFree\": false," +
                "    \"interestPayer\": null," +
                "    \"approveNo\": \"00000000\"," +
                "    \"useCardPoint\": false," +
                "    \"cardType\": \"신용\"," +
                "    \"ownerType\": \"개인\"," +
                "    \"acquireStatus\": \"READY\"," +
                "    \"amount\": 1000" +
                "  }," +
                "  \"virtualAccount\": null," +
                "  \"transfer\": null," +
                "  \"mobilePhone\": null," +
                "  \"giftCertificate\": null," +
                "  \"cashReceipt\": null," +
                "  \"cashReceipts\": null," +
                "  \"discount\": null," +
                "  \"cancels\": null," +
                "  \"secret\": null," +
                "  \"type\": \"NORMAL\"," +
                "  \"easyPay\": {" +
                "    \"provider\": \"토스페이\"," +
                "    \"amount\": 0," +
                "    \"discountAmount\": 0" +
                "  }," +
                "  \"country\": \"KR\"," +
                "  \"failure\": null," +
                "  \"isPartialCancelable\": true," +
                "  \"receipt\": {" +
                "    \"url\": \"https://dashboard.tosspayments.com/receipt/redirection?transactionId=tviva20240213121757MvuS8&ref=PX\"" +
                "  }," +
                "  \"checkout\": {" +
                "    \"url\": \"https://api.tosspayments.com/v1/payments/5EnNZRJGvaBX7zk2yd8ydw26XvwXkLrx9POLqKQjmAw4b0e1/checkout\"" +
                "  }," +
                "  \"currency\": \"KRW\"," +
                "  \"totalAmount\": 1000," +
                "  \"balanceAmount\": 1000," +
                "  \"suppliedAmount\": 909," +
                "  \"vat\": 91," +
                "  \"taxFreeAmount\": 0," +
                "  \"metadata\": null," +
                "  \"method\": \"카드\"," +
                "  \"version\": \"2022-11-16\"" +
                "}";

        TossPayment payment = objectMapper.readValue(json, TossPayment.class);

        Assert.assertEquals(payment.getPaymentKey(), "5EnNZRJGvaBX7zk2yd8ydw26XvwXkLrx9POLqKQjmAw4b0e1");
        Assert.assertEquals(payment.getOrderId(), "a4CWyWY5m89PNh7xJwhk1");
        Assert.assertEquals(payment.getStatus(), "DONE");
        Assert.assertEquals(payment.getTotalAmount(), Long.valueOf(1000));
        Assert.assertEquals(payment.getApprovedAt(), "2024-02-13T12:18:14+09:00");
        Assert.assertEquals(payment.getSuppliedAmount(), Long.valueOf(909));
        Assert.assertEquals(payment.getVat(), Long.valueOf(91));
        
        // Check new fields
        Assert.assertNotNull(payment.getReceipt());
        Assert.assertTrue(payment.getReceipt().getUrl().contains("transactionId=tviva20240213121757MvuS8"));
        
        Assert.assertNotNull(payment.getEasyPay());
        Assert.assertEquals(payment.getEasyPay().getProvider(), "토스페이");
    }

    @Test
    public void testErrorDeserialization() throws JsonProcessingException {
        String json = "{" +
                "\"code\":\"NOT_FOUND_PAYMENT\"," +
                "\"message\":\"존재하지 않는 결제입니다.\"" +
                "}";
        
        TossError error = objectMapper.readValue(json, TossError.class);
        
        Assert.assertEquals(error.getCode(), "NOT_FOUND_PAYMENT");
        Assert.assertEquals(error.getMessage(), "존재하지 않는 결제입니다.");
    }

    @Test
    public void testTossBillingDeserialization() throws Exception {
        String json = "{\n" +
                "  \"mId\": \"tosspayments\",\n" +
                "  \"customerKey\": \"test_customer_key\",\n" +
                "  \"authenticatedAt\": \"2021-01-01T10:00:00+09:00\",\n" +
                "  \"method\": \"카드\",\n" +
                "  \"billingKey\": \"test_billing_key\",\n" +
                "  \"card\": {\n" +
                "    \"issuerCode\": \"61\",\n" +
                "    \"acquirerCode\": \"31\",\n" +
                "    \"number\": \"43301234****123*\",\n" +
                "    \"chinaUnionPay\": false,\n" +
                "    \"cardType\": \"신용\",\n" +
                "    \"ownerType\": \"개인\"\n" +
                "  },\n" +
                "  \"transfers\": [\n" +
                "    {\n" +
                "      \"bankName\": \"토스뱅크\",\n" +
                "      \"bankAccountNumber\": \"123***789\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"cardCompany\": \"현대\",\n" +
                "  \"cardNumber\": \"43301234****123*\"\n" +
                "}";

        TossBilling billing = objectMapper.readValue(json, TossBilling.class);

        Assert.assertNotNull(billing);
        Assert.assertEquals(billing.getMId(), "tosspayments");
        Assert.assertEquals(billing.getCustomerKey(), "test_customer_key");
        Assert.assertEquals(billing.getMethod(), "카드");
        Assert.assertEquals(billing.getBillingKey(), "test_billing_key");
        
        // Card validation
        Assert.assertNotNull(billing.getCard());
        Assert.assertEquals(billing.getCard().getNumber(), "43301234****123*");
        Assert.assertEquals(billing.getCard().getCardType(), "신용");
        
        // Transfers validation
        Assert.assertNotNull(billing.getTransfers());
        Assert.assertEquals(billing.getTransfers().size(), 1);
        Assert.assertEquals(billing.getTransfers().get(0).getBankName(), "토스뱅크");
        Assert.assertEquals(billing.getTransfers().get(0).getBankAccountNumber(), "123***789");
        
        // Top-level card fields
        Assert.assertEquals(billing.getCardCompany(), "현대");
        Assert.assertEquals(billing.getCardNumber(), "43301234****123*");
    }
}
