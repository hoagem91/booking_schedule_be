package com.booking_schedule.examine.configuration;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter @Setter @Component
@ConfigurationProperties(prefix = "payment.payos")
public class PayosProperties {
    private boolean enabled;
    private String clientId = "";
    private String apiKey = "";
    private String checksumKey = "";
    private String returnUrl = "http://localhost:4200/payment/result?provider=payos";
    private String cancelUrl = "http://localhost:4200/payment/result?provider=payos&cancelled=true";
    private int expireMinutes = 15;
    @PostConstruct
    public void validate() {
        if (!enabled) return;
        if (clientId.isBlank() || apiKey.isBlank() || checksumKey.isBlank()
                || !validUrl(returnUrl) || !validUrl(cancelUrl) || expireMinutes < 1 || expireMinutes > 60)
            throw new IllegalStateException("Configure payOS keys, absolute HTTP(S) return/cancel URLs and expiry 1–60 minutes");
    }
    private boolean validUrl(String value) {
        try {
            var uri = java.net.URI.create(value);
            return uri.getHost() != null && uri.getUserInfo() == null
                    && ("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()));
        } catch (RuntimeException exception) { return false; }
    }
}
