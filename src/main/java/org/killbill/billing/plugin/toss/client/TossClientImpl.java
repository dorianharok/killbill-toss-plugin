package org.killbill.billing.plugin.toss.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.killbill.billing.plugin.toss.client.exception.TossApplicationException;
import org.killbill.billing.plugin.toss.client.model.BillingKeyPaymentRequest;
import org.killbill.billing.plugin.toss.client.model.BillingKeyRequest;
import org.killbill.billing.plugin.toss.client.model.PaymentCancelRequest;
import org.killbill.billing.plugin.toss.client.model.PaymentConfirmRequest;
import org.killbill.billing.plugin.toss.client.model.TossBilling;
import org.killbill.billing.plugin.toss.client.model.TossError;
import org.killbill.billing.plugin.toss.client.model.TossPayment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

public class TossClientImpl implements TossClient {

    private static final Logger logger = LoggerFactory.getLogger(TossClientImpl.class);
    private static final String DEFAULT_BASE_URL = "https://api.tosspayments.com/v1";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public TossClientImpl() {
        this(DEFAULT_BASE_URL);
    }

    public TossClientImpl(String baseUrl) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = baseUrl;
    }

    public TossClientImpl(HttpClient httpClient, ObjectMapper objectMapper, String baseUrl) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
    }

    @Override
    public TossPayment confirmPayment(final String secretKey, final PaymentConfirmRequest request, final String idempotencyKey) throws IOException, InterruptedException {
        final String requestBody = objectMapper.writeValueAsString(request);
        final HttpRequest httpRequest = buildRequest(secretKey, "/payments/confirm", "POST", requestBody, idempotencyKey);
        return execute(httpRequest, TossPayment.class);
    }

    @Override
    public TossPayment cancelPayment(final String secretKey, final String paymentKey, final PaymentCancelRequest request, final String idempotencyKey) throws IOException, InterruptedException {
        final String requestBody = objectMapper.writeValueAsString(request);
        final HttpRequest httpRequest = buildRequest(secretKey, "/payments/" + paymentKey + "/cancel", "POST", requestBody, idempotencyKey);
        return execute(httpRequest, TossPayment.class);
    }

    @Override
    public TossPayment getPayment(String secretKey, String paymentKey) throws IOException, InterruptedException {
        HttpRequest httpRequest = buildRequest(secretKey, "/payments/" + paymentKey, "GET", null);
        return execute(httpRequest, TossPayment.class);
    }

    @Override
    public TossBilling issueBillingKey(String secretKey, BillingKeyRequest request) throws IOException, InterruptedException {
        String requestBody = objectMapper.writeValueAsString(request);
        HttpRequest httpRequest = buildRequest(secretKey, "/billing/authorizations/issue", "POST", requestBody);
        return execute(httpRequest, TossBilling.class);
    }

    @Override
    public TossPayment executeBillingKeyPayment(final String secretKey, final String billingKey, final BillingKeyPaymentRequest request, final String idempotencyKey) throws IOException, InterruptedException {
        final String requestBody = objectMapper.writeValueAsString(request);
        final HttpRequest httpRequest = buildRequest(secretKey, "/billing/" + billingKey, "POST", requestBody, idempotencyKey);
        return execute(httpRequest, TossPayment.class);
    }

    private HttpRequest buildRequest(final String secretKey, final String path, final String method, final String jsonBody) {
        return buildRequest(secretKey, path, method, jsonBody, null);
    }

    private HttpRequest buildRequest(final String secretKey, final String path, final String method, final String jsonBody, final String idempotencyKey) {
        final String authHeader = "Basic " + Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        final HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json");

        if (idempotencyKey != null) {
            builder.header("Idempotency-Key", idempotencyKey);
        }

        if ("POST".equalsIgnoreCase(method)) {
            builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        } else if ("GET".equalsIgnoreCase(method)) {
            builder.GET();
        }

        return builder.build();
    }

    private <T> T execute(HttpRequest request, Class<T> clazz) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return objectMapper.readValue(response.body(), clazz);
        } else {
            TossError tossError;
            try {
                tossError = objectMapper.readValue(response.body(), TossError.class);
            } catch (Exception e) {
                // Return generic error if parsing fails
                logger.warn("Failed to parse error response: {}", response.body());
                tossError = new TossError("UNKNOWN_ERROR", "Unknown error occurred: " + response.body());
            }
            throw new TossApplicationException(tossError, response.statusCode());
        }
    }
}
