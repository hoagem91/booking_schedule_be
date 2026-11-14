package com.booking_schedule.examine.service;

import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClinicDirectAnswers {
    private final ClinicAssistantTools tools;

    public Optional<String> answer(String message) {
        String text = normalize(message);
        var faq = ClinicFaqAnswers.answer(text);
        if (faq.isPresent()) return faq;
        if (Set.of("xin chao", "chao", "hello", "hi").contains(text)) {
            return Optional.of("Xin chào! Bạn muốn xem chuyên khoa, giá dịch vụ hay hướng dẫn đặt lịch?");
        }
        if (Set.of("dat lich nhu the nao", "huong dan dat lich", "cach dat lich").contains(text)) {
            return Optional.of("Bạn hãy mở chức năng đặt lịch, chọn chuyên khoa/dịch vụ và làm theo hướng dẫn. Lịch hẹn chỉ được xác nhận sau khi hoàn tất trên hệ thống.");
        }
        if (Set.of("co chuyen khoa nao", "phong kham co chuyen khoa nao", "danh sach chuyen khoa",
                "cac chuyen khoa", "chuyen khoa", "phong kham co nhung chuyen khoa nao", "cho toi xem danh sach chuyen khoa").contains(text)) {
            var specialties = tools.listActiveSpecialties();
            return Optional.of(specialties.isEmpty() ? "Hiện chưa có chuyên khoa đang hoạt động."
                    : "Các chuyên khoa đang hoạt động:\n" + specialties.stream()
                            .map(item -> "• " + item.name()).collect(Collectors.joining("\n")));
        }
        // Only explicit catalog requests bypass AI; symptom and compound questions must not be misrouted.
        if (Set.of("dich vu", "danh sach dich vu", "bang gia", "bang gia dich vu",
                "gia dich vu", "cac dich vu").contains(text)) {
            return Optional.of(formatServices(tools.listPublicServices(null)));
        }
        if (Set.of("cam on", "cam on ban", "thank you", "thanks").contains(text)) {
            return Optional.of("Rất vui được hỗ trợ bạn. Bạn cần xem dịch vụ hay hướng dẫn đặt lịch thêm không?");
        }
        return answerCatalogQuestion(text);
    }

    public enum QuickAction {
        SPECIALTIES, SERVICES, BOOKING_HELP, CHAT_HELP, APPOINTMENT_HELP, CANCEL_HELP, RESCHEDULE_HELP,
        PAYMENT_HELP, VNPAY_HELP, VIETQR_HELP, PAYOS_HELP, PAYMENT_PENDING_HELP, PAYMENT_FAILED_HELP,
        PAYMENT_MISMATCH_HELP, REFUND_HELP, PROFILE_HELP, VISIT_PREPARATION_HELP, CONTACT_HELP
    }

    public String quickReply(QuickAction action) {
        var faq = ClinicFaqAnswers.action(action.name());
        if (faq.isPresent()) return faq.get();
        return answer(switch (action) {
            case SPECIALTIES -> "danh sách chuyên khoa";
            case SERVICES -> "bảng giá dịch vụ";
            case BOOKING_HELP -> "cách đặt lịch";
            default -> throw new IllegalArgumentException("Unknown quick action");
        }).orElseThrow();
    }

    private Optional<String> answerCatalogQuestion(String text) {
        // Anchored patterns deliberately reject symptom, comparison and follow-up questions.
        String clean = text.replaceFirst("^(cho toi hoi|cho minh hoi|toi muon biet|minh muon biet) +", "")
                .replaceFirst(" +(a|nhe|voi|duoc khong)$", "");
        var pricePrefix = java.util.regex.Pattern.compile("^(?:gia|chi phi)(?: dich vu)? (.+?)(?: la bao nhieu| bao nhieu| bao nhieu tien)?$").matcher(clean);
        var priceSuffix = java.util.regex.Pattern.compile("^(.+?) (?:gia bao nhieu|bao nhieu tien|het bao nhieu tien|het khoang bao nhieu|chi phi the nao)$").matcher(clean);
        var duration = java.util.regex.Pattern.compile("^(.+?) (?:mat bao lau|trong bao lau|keo dai bao lau|bao nhieu phut)$").matcher(clean);
        boolean durationOnly = duration.matches();
        String entity;
        if (durationOnly) entity = duration.group(1);
        else if (pricePrefix.matches()) entity = pricePrefix.group(1);
        else if (priceSuffix.matches()) entity = priceSuffix.group(1);
        else return Optional.empty();

        // Match the entire entity against real catalog names. Never infer a specialty from symptoms.
        var services = tools.listPublicServices(null);
        final String requested = entity;
        var exact = services.stream().filter(item -> normalize(item.name()).equals(requested)).toList();
        if (!exact.isEmpty()) return Optional.of(formatServices(exact, durationOnly));
        String specialty = entity.replaceFirst("^(?:kham chuyen khoa|chuyen khoa|kham) +", "");
        var matchingSpecialties = services.stream().filter(item -> item.specialtyName() != null
                && normalize(item.specialtyName()).equals(specialty)).toList();
        if (!matchingSpecialties.isEmpty()) return Optional.of(formatServices(matchingSpecialties, durationOnly));
        return Optional.empty();
    }

    private String formatServices(java.util.List<ClinicAssistantTools.ServiceInfo> services) {
        return formatServices(services, false);
    }

    private String formatServices(java.util.List<ClinicAssistantTools.ServiceInfo> services, boolean durationOnly) {
        if (services.isEmpty()) return "Hiện chưa có dịch vụ công khai phù hợp.";
        var currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
        return "Dịch vụ công khai:\n" + services.stream().limit(5)
                .map(item -> "• " + item.name() + (item.specialtyName() == null ? "" : " (" + item.specialtyName() + ")") + " — "
                        + (durationOnly
                            ? (item.durationMinutes() == null ? "Liên hệ phòng khám để biết thời lượng" : item.durationMinutes() + " phút")
                            : (item.price() == null ? "Liên hệ phòng khám để biết giá" : currency.format(item.price()))))
                .collect(Collectors.joining("\n"))
                + (services.size() > 5 ? "\nXem mục dịch vụ để biết danh sách đầy đủ." : "");
    }

    static String normalize(String text) {
        return Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('đ', 'd')
                .replaceAll("[?!.,]+$", "").trim().replaceAll("\\s+", " ");
    }
}
