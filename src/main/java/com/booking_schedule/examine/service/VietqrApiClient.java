package com.booking_schedule.examine.service;
import com.booking_schedule.examine.configuration.VietqrProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.MediaType;
import java.net.http.HttpClient;
import java.time.Duration;
import java.math.BigDecimal;
import java.util.Map;
@Component
public class VietqrApiClient {
    private final VietqrProperties properties;
    private final RestClient client;
    @org.springframework.beans.factory.annotation.Autowired
    public VietqrApiClient(VietqrProperties properties) {
        this.properties=properties;
        var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        client=RestClient.builder().requestFactory(factory).build();
    }
    VietqrApiClient(VietqrProperties properties, RestClient client) {
        this.properties=properties;
        this.client=client;
    }
    public JsonNode generate(String reference, BigDecimal amount) {
        var p=properties;
        if (!p.getApiBaseUrl().startsWith("https://") || p.getApiUsername().isBlank() || p.getApiPassword().isBlank()
                || p.getBankAccount().isBlank() || p.getBankCode().isBlank() || p.getUserBankName().isBlank())
            throw new IllegalStateException("VietQR credentials and bank information must be configured");
        JsonNode token=client.post().uri(p.getApiBaseUrl()+"/vqr/api/token_generate")
                .headers(h -> h.setBasicAuth(p.getApiUsername(),p.getApiPassword()))
                .retrieve().body(JsonNode.class);
        if(token==null || token.path("access_token").asText().isBlank()) throw new IllegalStateException("VietQR token unavailable");
        JsonNode qr=client.post().uri(p.getApiBaseUrl()+"/vqr/api/qr/generate-customer")
                .headers(h -> h.setBearerAuth(token.path("access_token").asText())).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("bankCode",p.getBankCode(),"bankAccount",p.getBankAccount(),"userBankName",p.getUserBankName(),
                        "amount",amount.longValueExact(),"content","KHAM"+reference,"orderId",reference,"qrType",0,"transType","C","reconciliation",true))
                .retrieve().body(JsonNode.class);
        if(qr==null || qr.path("qrCode").asText().isBlank()) throw new IllegalStateException("VietQR QR response invalid");
        if (qr.path("existing").asInt(0) == 1
                || (qr.hasNonNull("bankAccount") && !p.getBankAccount().equals(qr.path("bankAccount").asText()))
                || (qr.hasNonNull("orderId") && !reference.equals(qr.path("orderId").asText()))
                || (qr.hasNonNull("amount") && new BigDecimal(qr.path("amount").asText()).compareTo(amount)!=0))
            throw new IllegalStateException("VietQR response does not match the requested payment");
        return qr;
    }
}
