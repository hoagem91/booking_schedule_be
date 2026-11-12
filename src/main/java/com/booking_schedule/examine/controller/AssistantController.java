package com.booking_schedule.examine.controller;

import com.booking_schedule.examine.dto.request.AssistantChatRequest;
import com.booking_schedule.examine.dto.response.ApiResponse;
import com.booking_schedule.examine.service.AssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {
    private final AssistantService assistantService;

    @PostMapping("/chat")
    public ApiResponse<String> chat(@Valid @RequestBody AssistantChatRequest request) {
        return ApiResponse.<String>builder()
                .message("Assistant response generated")
                .result(assistantService.chat(request.message(), request.history()))
                .build();
    }
    public record QuickReplyRequest(
            @jakarta.validation.constraints.NotNull
            com.booking_schedule.examine.service.ClinicDirectAnswers.QuickAction action) {}

    @PostMapping("/quick-reply")
    public ApiResponse<String> quickReply(@Valid @RequestBody QuickReplyRequest request) {
        return ApiResponse.<String>builder()
                .message("Assistant response generated")
                .result(assistantService.quickReply(request.action()))
                .build();
    }
}
