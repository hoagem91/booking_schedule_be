package com.booking_schedule.examine.service;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AssistantService {
    static final String FALLBACK = "Trợ lý hiện chưa thể trả lời. Bạn có thể xem mục chuyên khoa, dịch vụ hoặc tiếp tục qua chức năng đặt lịch.";
    private final ClinicDirectAnswers direct;
    private final GroqAssistantClient groq;
    private final long timeoutMillis;
    // No waiting queue: overload returns immediately instead of accumulating latency.
    private final ExecutorService executor = new ThreadPoolExecutor(0, 8, 30, TimeUnit.SECONDS,
            new SynchronousQueue<>(), runnable -> {
                Thread thread = new Thread(runnable, "assistant-worker");
                thread.setDaemon(true);
                return thread;
            });

    public AssistantService(ClinicDirectAnswers direct, GroqAssistantClient groq,
            @Value("${assistant.request-timeout-ms:10000}") long timeoutMillis) {
        this.direct = direct;
        this.groq = groq;
        this.timeoutMillis = timeoutMillis;
    }

    public String chat(String message) {
        return chat(message, java.util.List.of());
    }

    public String chat(String message, java.util.List<com.booking_schedule.examine.dto.request.AssistantChatRequest.Turn> history) {
        return execute(lookup -> direct.answer(message).orElseGet(() -> groq.answer(message, history, lookup::set)));
    }

    public String quickReply(ClinicDirectAnswers.QuickAction action) {
        return execute(lookup -> direct.quickReply(action));
    }

    private String execute(java.util.function.Function<java.util.concurrent.atomic.AtomicReference<String>, String> operation) {
        long started = System.nanoTime();
        Future<String> task = null;
        var lookup = new java.util.concurrent.atomic.AtomicReference<String>(FALLBACK);
        try {
            task = executor.submit(() -> operation.apply(lookup));
            return task.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return FALLBACK;
        } catch (TimeoutException | ExecutionException | RejectedExecutionException exception) {
            log.warn("Assistant fallback: {}", exception.getClass().getSimpleName());
            return lookup.get();
        } finally {
            if (task != null && !task.isDone()) task.cancel(true);
            log.info("Assistant request completed in {} ms", (System.nanoTime() - started) / 1_000_000);
        }
    }

    @PreDestroy
    void shutdown() { executor.shutdownNow(); }
}
