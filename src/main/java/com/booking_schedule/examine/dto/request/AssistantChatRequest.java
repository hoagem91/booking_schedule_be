package com.booking_schedule.examine.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record AssistantChatRequest(
        @NotBlank(message = "INVALID_KEY") @Size(max = 2000, message = "INVALID_KEY") String message,
        @Size(max = 8) List<@NotNull @Valid Turn> history) {
    public AssistantChatRequest(String message) { this(message, List.of()); }
    public record Turn(
            @NotNull @Pattern(regexp = "user|assistant") String role,
            @NotBlank @Size(max = 2000) String content) {}
}
