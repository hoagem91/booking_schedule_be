package com.booking_schedule.examine.service;

import com.booking_schedule.examine.configuration.PayosProperties;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import vn.payos.PayOS;
import vn.payos.core.ClientOptions;
import vn.payos.model.v2.paymentRequests.*;
import vn.payos.model.webhooks.WebhookData;

@Component
@lombok.extern.slf4j.Slf4j
public class PayosGateway {
    private final PayosProperties properties;
    private final PayOS client;
    private final ObjectMapper json = JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
    @Autowired
    public PayosGateway(PayosProperties properties) {
        this.properties = properties;
        properties.validate();
        client = properties.isEnabled() ? new PayOS(ClientOptions.builder()
                .clientId(properties.getClientId()).apiKey(properties.getApiKey()).checksumKey(properties.getChecksumKey())
                .timeoutMs(10000).maxRetries(0).build()) : null;
    }
    PayosGateway(PayosProperties properties, PayOS client) { this.properties=properties; this.client=client; }
    public void requireEnabled() {
        if (!properties.isEnabled()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "PAYOS_DISABLED");
    }
    public CreatePaymentLinkResponse create(String reference, BigDecimal amount, Instant expiresAt) {
        requireEnabled();
        // Keep description within the 9-character limit for channels without direct bank linkage.
        return client.paymentRequests().create(CreatePaymentLinkRequest.builder()
                .orderCode(Long.valueOf(reference)).amount(amount.longValueExact()).description("KHAM")
                .returnUrl(properties.getReturnUrl()).cancelUrl(properties.getCancelUrl())
                .expiredAt(expiresAt.getEpochSecond()).build());
    }
    public PaymentLink get(String reference) {
        requireEnabled();
        return client.paymentRequests().get(Long.valueOf(reference));
    }
    public WebhookData verify(String raw) {
        requireEnabled();
        String stage = "payload";
        try {
            if (raw.length() > 65536) throw new IllegalArgumentException();
            JsonNode root = json.readTree(raw);
            JsonNode data = root.path("data");
            if (!data.isObject() || !root.path("signature").isTextual()) throw new IllegalArgumentException();
            // Sign ALL original data fields, including unknown fields; avoid lossy DTO verification.
            stage = "signature";
            String expected = client.getCrypto().createSignatureFromObj(json.convertValue(data, Map.class), properties.getChecksumKey());
            String received = root.path("signature").asText();
            if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII), received.getBytes(StandardCharsets.US_ASCII)))
                throw new IllegalArgumentException();
            stage = "status";
            // The channel-registration webhook may omit success. Signed data.code remains mandatory.
            if ((root.has("success") && (!root.path("success").isBoolean() || !root.path("success").asBoolean()))
                    || !"00".equals(root.path("code").asText()) || !"00".equals(data.path("code").asText()))
                throw new IllegalArgumentException();
            stage = "schema";
            if (!data.path("orderCode").isIntegralNumber() || !data.path("orderCode").canConvertToLong()
                    || data.path("orderCode").asLong() <= 0 || !data.path("amount").isIntegralNumber()
                    || !data.path("amount").canConvertToLong() || data.path("amount").asLong() <= 0)
                throw new IllegalArgumentException();
            WebhookData event = json.treeToValue(data, WebhookData.class);
            if (!"VND".equals(event.getCurrency()) || event.getPaymentLinkId().isBlank()
                    || event.getPaymentLinkId().length()>100 || event.getReference().isBlank()
                    || event.getReference().length()>100 || event.getAccountNumber().isBlank()) throw new IllegalArgumentException();
            return event;
        } catch (Exception exception) {
            log.warn("payOS webhook rejected at {} ({})", stage, exception.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_PAYOS_WEBHOOK");
        }
    }
    @PreDestroy void close() { if (client != null) client.close(); }
}
