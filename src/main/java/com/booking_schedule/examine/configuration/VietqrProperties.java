package com.booking_schedule.examine.configuration;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.properties.ConfigurationProperties;
@Data @Component @ConfigurationProperties(prefix="payment.vietqr")
public class VietqrProperties {
    private boolean enabled;
    private String apiBaseUrl = "https://dev.vietqr.org";
    private String apiUsername = "", apiPassword = "", tokenUsername = "", tokenPassword = "";
    private String bankCode = "", bankAccount = "", userBankName = "", webhookSecret = "";
    private int webhookTokenTtlSeconds = 300;
    @jakarta.annotation.PostConstruct
    public void validateEnabledConfiguration() {
        if (!enabled) return;
        if (java.util.stream.Stream.of(apiUsername, apiPassword, tokenUsername, tokenPassword,
                bankCode, bankAccount, userBankName).anyMatch(value -> value == null || value.isBlank())
                || webhookSecret == null || webhookSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 64
                || apiBaseUrl == null || !apiBaseUrl.startsWith("https://"))
            throw new IllegalStateException("VietQR enabled but API, bank or webhook credentials are missing; webhook secret requires at least 64 bytes");
    }
}
