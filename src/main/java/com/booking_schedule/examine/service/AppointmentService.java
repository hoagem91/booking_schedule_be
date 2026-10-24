package com.booking_schedule.examine.service;

import com.booking_schedule.examine.dto.request.AppointmentCreateRequest;
import com.booking_schedule.examine.dto.response.AppointmentResponse;
import com.booking_schedule.examine.dto.response.AppointmentSummaryResponse;
import com.booking_schedule.examine.dto.request.AppointmentSearchRequest;
import com.booking_schedule.examine.entity.*;
import com.booking_schedule.examine.enums.*;
import com.booking_schedule.examine.exception.*;
import com.booking_schedule.examine.mapper.AppointmentMapper;
import com.booking_schedule.examine.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentService {
    private static final List<EAppointmentStatus> ACTIVE = List.of(EAppointmentStatus.PENDING,
            EAppointmentStatus.CONFIRMED, EAppointmentStatus.IN_PROGRESS);
    private final AppointmentRepository appointments;
    private final AppointmentSummaryRepository appointmentSummary;
    private final PatientRepository patients;
    private final UserRepository users;
    private final ServiceRepository services;
    private final TimeSlotRepository slots;
    private final DoctorScheduleRepository schedules;
    private final PromotionRepository promotions;
    private final PaymentRepository payments;
    private final ReviewRepository reviews;
    private final SystemSettingRepository settings;
    private final TimeSlotService timeSlots;
    private final AppointmentLifecycle lifecycle;
    private final AppointmentMapper mapper;

    @Transactional
    public AppointmentResponse createAppointment(AppointmentCreateRequest request) {
        Patient patient = currentPatient();

        if (request.getPatientId() != null && !request.getPatientId().equals(patient.getId()))
            throw new AppException(ErrorCode.UNAUTHORIZED);
        return create(request, patient.getId());
    }

    @Transactional
    public AppointmentResponse createForPatient(AppointmentCreateRequest request) {
        if (request.getPatientId() == null) throw new AppException(ErrorCode.APPOINTMENT_INVALID_REQUEST);
        return create(request, request.getPatientId());
    }

    private AppointmentResponse create(AppointmentCreateRequest request, Long patientId) {
        if (request.getDoctorId() == null || request.getServiceId() == null || request.getTimeSlotId() == null
                || request.getAppointmentDate() == null || request.getAppointmentDate().isBefore(LocalDate.now()))
            throw new AppException(ErrorCode.APPOINTMENT_INVALID_REQUEST);
        // Serialize bookings for one patient, including different schedules.
        Patient patient = patients.findLockedById(patientId).orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND));
        if (patient.getUser() == null || !"ACTIVE".equals(patient.getUser().getStatus()))
            throw new AppException(ErrorCode.USER_INACTIVE);
        Long scheduleId = slots.findScheduleIdBySlotId(request.getTimeSlotId())
                .orElseThrow(() -> new AppException(ErrorCode.TIME_SLOT_NOT_FOUND));
        DoctorSchedule schedule = schedules.findLockedById(scheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));
        List<TimeSlot> all = slots.findAllLockedByScheduleId(scheduleId);
        TimeSlot slot = all.stream().filter(s -> s.getId().equals(request.getTimeSlotId())).findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.TIME_SLOT_NOT_FOUND));
        Doctor doctor = schedule.getDoctor();
        if (!doctor.getId().equals(request.getDoctorId()) || !schedule.getWorkDate().equals(request.getAppointmentDate()))
            throw new AppException(ErrorCode.APPOINTMENT_SLOT_MISMATCH);
        if (doctor.getStatus() != EStatus.ACTIVE) throw new AppException(ErrorCode.DOCTOR_INACTIVE);
        if (doctor.getUser() == null || !"ACTIVE".equals(doctor.getUser().getStatus()))
            throw new AppException(ErrorCode.DOCTOR_INACTIVE);
        if (doctor.getSpecialty().getStatus() != EStatus.ACTIVE) throw new AppException(ErrorCode.SPECIALTY_INACTIVE);
        com.booking_schedule.examine.entity.Service service = services.findById(request.getServiceId())
                .orElseThrow(() -> new AppException(ErrorCode.SERVICE_NOT_EXISTED));
        if (service.getStatus() != EStatus.ACTIVE) throw new AppException(ErrorCode.SERVICE_INACTIVE);
        // Services without a specialty are available to all specialties.
        if (service.getSpecialty() != null && !service.getSpecialty().getId().equals(doctor.getSpecialty().getId()))
            throw new AppException(ErrorCode.APPOINTMENT_SPECIALTY_MISMATCH);
        if (!appointments.findOverlaps(patientId, schedule.getWorkDate(), ACTIVE, slot.getStartTime(), slot.getEndTime()).isEmpty())
            throw new AppException(ErrorCode.APPOINTMENT_DUPLICATE);
        BigDecimal price = service.getPrice();
        if (price == null || price.signum() < 0) throw new AppException(ErrorCode.APPOINTMENT_PRICE_UNAVAILABLE);
        // Keep the same lock order as cancellation: schedule, slots, promotion.
        timeSlots.reserveSlot(slot.getId());
        Promotion promotion = request.getPromotionId() == null ? null : reservePromotion(request.getPromotionId());
        BigDecimal amount = discountedAmount(price, promotion);
        Appointment appointment = appointments.saveAndFlush(Appointment.builder().appointmentCode(code("LH"))
                .patient(patient).doctor(doctor).service(service).timeSlot(slot).appointmentDate(schedule.getWorkDate())
                .reason(request.getReason()).note(request.getNote()).promotion(promotion).finalAmount(amount).build());
        lifecycle.notifyPatient(appointment, "Đặt lịch hẹn thành công", "APPOINTMENT_CREATED");
        return response(appointment);
    }

    private Promotion reservePromotion(Long id) {
        Promotion promotion = promotions.findLockedById(id).orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now();
        int count = promotion.getUsedCount() == null ? 0 : promotion.getUsedCount();
        if (!"ACTIVE".equals(promotion.getStatus()) || count < 0 || count == Integer.MAX_VALUE
                || (promotion.getStartDate() != null && now.isBefore(promotion.getStartDate()))
                || (promotion.getEndDate() != null && now.isAfter(promotion.getEndDate()))
                || (promotion.getUsageLimit() != null && count >= promotion.getUsageLimit())
                || promotion.getDiscountType() == null || promotion.getDiscountValue() == null
                || promotion.getDiscountValue().signum() < 0
                || (promotion.getDiscountType() == EDiscountType.PERCENT && promotion.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0)
                || (promotion.getMaxDiscount() != null && promotion.getMaxDiscount().signum() < 0))
            throw new AppException(ErrorCode.PROMOTION_INVALID);
        promotion.setUsedCount(count + 1);
        return promotion;
    }

    private BigDecimal discountedAmount(BigDecimal price, Promotion promotion) {
        BigDecimal discount = BigDecimal.ZERO;
        if (promotion != null) {
            discount = promotion.getDiscountType() == EDiscountType.PERCENT
                    ? price.multiply(promotion.getDiscountValue()).divide(BigDecimal.valueOf(100)) : promotion.getDiscountValue();
            if (promotion.getMaxDiscount() != null) discount = discount.min(promotion.getMaxDiscount());
        }
        return price.subtract(discount.min(price)).setScale(2, RoundingMode.HALF_UP);
    }

    public Patient currentPatient() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated())
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        User user = users.findByUsername(authentication.getName()).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        log.info("Username:{}", user.getUsername());
        return patients.findByUserId(user.getId()).orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND));
    }

    public AppointmentResponse getMine(Long id) {
        Appointment appointment = find(id);
        requireOwner(appointment);
        return response(appointment);
    }

    public AppointmentResponse get(Long id) {
        return response(find(id));
    }

    public Page<AppointmentResponse> history(Pageable pageable) {
        return search(null, null, currentPatient().getId(), null, null, null, pageable);
    }

    public Page<AppointmentResponse> search(String keyword, Long doctorId, Long patientId, EAppointmentStatus status,
                                            LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        return appointments.findAll(searchSpecification(keyword, doctorId, patientId, status, fromDate, toDate), pageable)
                .map(this::response);
    }

    public AppointmentSummaryResponse summary(AppointmentSearchRequest request) {
        var spec = searchSpecification(request.getKeyword(), request.getDoctorId(), request.getPatientId(),
                request.getStatus(), request.getFromDate(), request.getToDate());
        Map<Long, Map<EAppointmentStatus, Long>> counts = new HashMap<>();
        Map<EAppointmentStatus, Long> statusCounts = emptyStatusCounts();
        for (var row : appointmentSummary.countByDoctorAndStatus(spec)) {
            counts.computeIfAbsent(row.doctorId(), id -> emptyStatusCounts()).put(row.status(), row.count());
            statusCounts.merge(row.status(), row.count(), Long::sum);
        }
        var doctors = appointmentSummary.findDoctors(request.getDoctorId()).stream().map(doctor -> {
            var doctorCounts = counts.getOrDefault(doctor.doctorId(), emptyStatusCounts());
            long total = doctorCounts.values().stream().mapToLong(Long::longValue).sum();
            return new AppointmentSummaryResponse.DoctorSummary(doctor.doctorId(), doctor.doctorCode(),
                    doctor.doctorName(), total, doctorCounts);
        }).toList();
        return new AppointmentSummaryResponse(statusCounts.values().stream().mapToLong(Long::longValue).sum(),
                statusCounts, doctors);
    }

    private Map<EAppointmentStatus, Long> emptyStatusCounts() {
        Map<EAppointmentStatus, Long> counts = new EnumMap<>(EAppointmentStatus.class);
        for (var status : EAppointmentStatus.values()) counts.put(status, 0L);
        return counts;
    }

    private Specification<Appointment> searchSpecification(String keyword, Long doctorId, Long patientId,
                                                           EAppointmentStatus status, LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate))
            throw new AppException(ErrorCode.APPOINTMENT_INVALID_REQUEST);
        Specification<Appointment> spec = (root, query, cb) -> cb.conjunction();
        if (doctorId != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("doctor").get("id"), doctorId));
        if (patientId != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("patient").get("id"), patientId));
        if (status != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), status));
        if (fromDate != null)
            spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("appointmentDate"), fromDate));
        if (toDate != null) spec = spec.and((r, q, cb) -> cb.lessThanOrEqualTo(r.get("appointmentDate"), toDate));
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            spec = spec.and((r, q, cb) -> cb.or(cb.like(cb.lower(r.get("appointmentCode")), pattern, '\\'),
                    cb.like(cb.lower(r.get("patient").get("user").get("fullName")), pattern, '\\')));
        }
        return spec;
    }

    @Transactional
    public AppointmentResponse cancelMine(Long id, String note) {
        Appointment appointment = lock(id);
        requireOwner(appointment);
        if (appointment.getStatus() == EAppointmentStatus.CANCELLED) return response(appointment);
        if (!List.of(EAppointmentStatus.PENDING, EAppointmentStatus.CONFIRMED).contains(appointment.getStatus()))
            throw new AppException(ErrorCode.APPOINTMENT_INVALID_TRANSITION);
        long hours = cancellationHours();
        LocalDateTime deadline = LocalDateTime.of(appointment.getAppointmentDate(), appointment.getTimeSlot().getStartTime()).minusHours(hours);
        if (LocalDateTime.now().isAfter(deadline)) throw new AppException(ErrorCode.APPOINTMENT_CANCEL_TOO_LATE);
        lifecycle.cancel(appointment, note, false);
        appointments.flush();
        return response(appointment);
    }

    @Transactional
    public AppointmentResponse transition(Long id, EAppointmentStatus target, String note) {
        Appointment appointment = lock(id);
        if (target == null) throw new AppException(ErrorCode.APPOINTMENT_INVALID_TRANSITION);
        if (appointment.getStatus() == target) return response(appointment);
        if (target == EAppointmentStatus.CANCELLED) {
            lifecycle.cancel(appointment, note, false);
            appointments.flush();
            return response(appointment);
        }
        EAppointmentStatus old = appointment.getStatus();
        boolean valid = (target == EAppointmentStatus.CONFIRMED && old == EAppointmentStatus.PENDING)
                || (target == EAppointmentStatus.IN_PROGRESS && old == EAppointmentStatus.CONFIRMED)
                || (target == EAppointmentStatus.COMPLETED && old == EAppointmentStatus.IN_PROGRESS)
                || (target == EAppointmentStatus.NO_SHOW && (old == EAppointmentStatus.PENDING || old == EAppointmentStatus.CONFIRMED));
        if (!valid) throw new AppException(ErrorCode.APPOINTMENT_INVALID_TRANSITION);
        LocalDateTime now = LocalDateTime.now();
        if ((target == EAppointmentStatus.IN_PROGRESS && now.isBefore(LocalDateTime.of(appointment.getAppointmentDate(), appointment.getTimeSlot().getStartTime())))
                || (target == EAppointmentStatus.NO_SHOW && now.isBefore(LocalDateTime.of(appointment.getAppointmentDate(), appointment.getTimeSlot().getEndTime()))))
            throw new AppException(ErrorCode.APPOINTMENT_TOO_EARLY);
        if (target == EAppointmentStatus.COMPLETED && payments.findByAppointmentId(id).isEmpty()) {
            BigDecimal amount = appointment.getFinalAmount();
            if (amount == null && appointment.getPromotion() == null) amount = appointment.getServicePrice();
            if (amount == null || amount.signum() < 0) throw new AppException(ErrorCode.APPOINTMENT_PRICE_UNAVAILABLE);
            payments.save(Payment.builder().paymentCode(code("TT")).appointment(appointment).amount(amount)
                    .paymentMethod("CASH").paymentStatus(EPaymentStatus.PENDING).build());
        }
        appointment.setStatus(target);
        if (note != null) appointment.setNote(note);
        lifecycle.notifyPatient(appointment, "Cập nhật lịch hẹn", "APPOINTMENT_" + target);
        appointments.flush();
        return response(appointment);
    }

    private long cancellationHours() {
        String value = settings.findBySettingKey(ESettingKey.CANCEL_BEFORE_HOURS).map(SystemSetting::getSettingValue).orElse("24");
        try {
            long hours = Long.parseLong(value.trim());
            if (hours < 0 || hours > 876000) throw new NumberFormatException();
            return hours;
        } catch (NumberFormatException ex) {
            throw new AppException(ErrorCode.APPOINTMENT_INVALID_SETTING);
        }
    }

    /**
     * Parent first to serialize with schedule cancellation, then the appointment.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public Appointment lock(Long id) {
        Long scheduleId = appointments.findScheduleIdByAppointmentId(id).orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));
        schedules.findLockedById(scheduleId).orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));
        slots.findAllLockedByScheduleId(scheduleId);
        return appointments.findLockedById(id).orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));
    }

    public void requireOwner(Appointment appointment) {
        if (!appointment.getPatient().getId().equals(currentPatient().getId()))
            throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    private Appointment find(Long id) {
        return appointments.findById(id).orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));
    }

    private AppointmentResponse response(Appointment appointment) {
        AppointmentResponse response = mapper.toAppointmentResponse(appointment);
        response.setReviewEligible(appointment.getStatus() == EAppointmentStatus.COMPLETED && !reviews.existsByAppointmentId(appointment.getId()));
        return response;
    }

    private String code(String prefix) {
        return ProfileCodeGenerator.generate(prefix);
    }
}
