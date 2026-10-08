package com.booking_schedule.examine.service;

import com.booking_schedule.examine.dto.request.*;
import com.booking_schedule.examine.dto.response.DoctorScheduleResponse;
import com.booking_schedule.examine.entity.*;
import com.booking_schedule.examine.enums.*;
import com.booking_schedule.examine.exception.*;
import com.booking_schedule.examine.mapper.DoctorScheduleMapper;
import com.booking_schedule.examine.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorScheduleService {
    private final DoctorScheduleRepository schedules;
    private final DoctorRepository doctors;
    private final RoomRepository rooms;
    private final AppointmentRepository appointments;
    private final NotificationRepository notifications;
    private final DoctorScheduleMapper mapper;

    @Transactional
    public DoctorScheduleResponse createDoctorSchedule(DoctorScheduleCreateRequest request) {
        Doctor doctor = lockDoctor(request.getDoctorId());
        requireActive(doctor);
        Room room = resolveRoom(request.getRoomId());
        int capacity = request.getMaxPatients() == null ? 20 : request.getMaxPatients();
        validateTime(request.getWorkDate(), request.getStartTime(), request.getEndTime(), capacity);
        DoctorSchedule schedule = mapper.toDoctorSchedule(request);
        schedule.setDoctor(doctor);
        schedule.setRoom(room);
        schedule.setMaxPatients(capacity);
        checkConflicts(schedule);
        generateSlots(schedule);
        return mapper.toDoctorScheduleResponse(schedules.saveAndFlush(schedule));
    }

    @Transactional
    public DoctorScheduleResponse updateDoctorSchedule(Long id, DoctorScheduleUpdateRequest request) {
        DoctorSchedule schedule = locked(id);
        if (request.getDoctorId() != null && !request.getDoctorId().equals(schedule.getDoctor().getId())) {
            throw new AppException(ErrorCode.SCHEDULE_DOCTOR_MISMATCH);
        }
        lockDoctor(schedule.getDoctor().getId());
        LocalDate date = request.getWorkDate() == null ? schedule.getWorkDate() : request.getWorkDate();
        LocalTime start = request.getStartTime() == null ? schedule.getStartTime() : request.getStartTime();
        LocalTime end = request.getEndTime() == null ? schedule.getEndTime() : request.getEndTime();
        int capacity = request.getMaxPatients() == null ? schedule.getMaxPatients() : request.getMaxPatients();
        boolean changed = !date.equals(schedule.getWorkDate()) || !start.equals(schedule.getStartTime()) || !end.equals(schedule.getEndTime());
        // Acquire room lock before any ordinary read establishes a repeatable-read snapshot.
        Room targetRoom = request.getRoomId() != null ? resolveRoom(request.getRoomId()) : schedule.getRoom();
        if (request.getRoomId() == null && targetRoom != null) rooms.findLockedById(targetRoom.getId());
        boolean booked = hasHistory(schedule);
        if (booked && (changed || capacity < schedule.getMaxPatients()))
            throw new AppException(ErrorCode.SCHEDULE_HAS_BOOKINGS);
        if (changed || !booked) validateTime(date, start, end, capacity);
        if (capacity < 1) throw new AppException(ErrorCode.SCHEDULE_INVALID_TIME);
        schedule.setRoom(targetRoom);
        schedule.setWorkDate(date);
        schedule.setStartTime(start);
        schedule.setEndTime(end);
        schedule.setMaxPatients(capacity);
        if (schedule.getStatus() != EStatusDoctorSchedule.CANCELLED) checkConflicts(schedule);
        // Keep slot IDs when only room/capacity changes.
        if (changed) {
            schedule.getTimeSlots().clear();
            schedules.flush();
            generateSlots(schedule);
        } else {
            allocateSlotCapacity(schedule, booked);
        }
        synchronizeSlots(schedule);
        return mapper.toDoctorScheduleResponse(schedules.saveAndFlush(schedule));
    }

    @Transactional
    public String deleteDoctorSchedule(Long id) {
        DoctorSchedule schedule = locked(id);
        lockDoctor(schedule.getDoctor().getId());
        if (!hasHistory(schedule)) {
            schedules.delete(schedule);
            schedules.flush();
            return "Đã xóa ca làm việc và các khung giờ";
        }
        cancel(schedule);
        schedules.saveAndFlush(schedule);
        return "Đã hủy ca; giữ lịch sử và tạo thông báo cho lịch hẹn còn hiệu lực";
    }

    @Transactional
    public DoctorScheduleResponse changeStatus(Long id, EStatusDoctorSchedule status) {
        if (status == null) throw new AppException(ErrorCode.SCHEDULE_INVALID_STATUS);
        DoctorSchedule schedule = locked(id);
        lockDoctor(schedule.getDoctor().getId());
        if (schedule.getStatus() == EStatusDoctorSchedule.CANCELLED && status != EStatusDoctorSchedule.CANCELLED)
            throw new AppException(ErrorCode.SCHEDULE_INVALID_STATUS);
        if (status == EStatusDoctorSchedule.CANCELLED) cancel(schedule);
        else {
            if (status == EStatusDoctorSchedule.OPEN) {
                requireActive(schedule.getDoctor());
                validateTime(schedule.getWorkDate(), schedule.getStartTime(), schedule.getEndTime(), schedule.getMaxPatients());
                if (schedule.getRoom() != null) resolveRoom(schedule.getRoom().getId());
                checkConflicts(schedule);
                allocateSlotCapacity(schedule, hasHistory(schedule));
            }
            schedule.setStatus(status);
            synchronizeSlots(schedule);
        }
        return mapper.toDoctorScheduleResponse(schedules.saveAndFlush(schedule));
    }

    public DoctorScheduleResponse getDoctorSchedule(Long id) {
        return mapper.toDoctorScheduleResponse(schedules.findById(id).orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND)));
    }

    public Page<DoctorScheduleResponse> search(Long doctorId, Long specialtyId, LocalDate fromDate,
                                               LocalDate toDate, EStatusDoctorSchedule status, Pageable pageable) {
        return schedules.findAll(filters(doctorId, specialtyId, fromDate, toDate, status), pageable).map(mapper::toDoctorScheduleResponse);
    }

    public List<DoctorScheduleResponse> publicSchedules(Long doctorId, Long specialtyId, LocalDate fromDate, LocalDate toDate) {
        LocalDate today = LocalDate.now();
        Specification<DoctorSchedule> filter = filters(doctorId, specialtyId, fromDate, toDate, EStatusDoctorSchedule.OPEN);
        filter = filter.and((root, query, cb) -> cb.and(cb.greaterThanOrEqualTo(root.get("workDate"), today),
                cb.equal(root.get("doctor").get("status"), EStatus.ACTIVE),
                cb.equal(root.get("doctor").get("specialty").get("status"), EStatus.ACTIVE)));
        LocalDateTime now = LocalDateTime.now();
        return schedules.findAll(filter, Sort.by("workDate", "startTime", "id")).stream()
                .filter(s -> bookedCount(s) < s.getMaxPatients())
                .map(s -> {
                    DoctorScheduleResponse response = mapper.toDoctorScheduleResponse(s);
                    response.setTimeSlots(s.getTimeSlots().stream()
                            .filter(slot -> TimeSlotPolicy.available(s, slot, s.getTimeSlots(), now))
                            .sorted(Comparator.comparing(TimeSlot::getStartTime)).map(mapper::toTimeSlotResponse).toList());
                    return response;
                }).filter(s -> !s.getTimeSlots().isEmpty()).toList();
    }

    private Specification<DoctorSchedule> filters(Long doctorId, Long specialtyId, LocalDate from, LocalDate to, EStatusDoctorSchedule status) {
        if (from != null && to != null && from.isAfter(to)) throw new AppException(ErrorCode.SCHEDULE_INVALID_TIME);
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (doctorId != null) predicates.add(cb.equal(root.get("doctor").get("id"), doctorId));
            if (specialtyId != null)
                predicates.add(cb.equal(root.get("doctor").get("specialty").get("id"), specialtyId));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("workDate"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("workDate"), to));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private void checkConflicts(DoctorSchedule schedule) {
        Specification<DoctorSchedule> overlap = (root, query, cb) -> cb.and(
                cb.equal(root.get("workDate"), schedule.getWorkDate()), cb.notEqual(root.get("status"), EStatusDoctorSchedule.CANCELLED),
                cb.lessThan(root.get("startTime"), schedule.getEndTime()), cb.greaterThan(root.get("endTime"), schedule.getStartTime()),
                schedule.getId() == null ? cb.conjunction() : cb.notEqual(root.get("id"), schedule.getId()));
        if (schedules.exists(overlap.and((root, query, cb) -> cb.equal(root.get("doctor").get("id"), schedule.getDoctor().getId()))))
            throw new AppException(ErrorCode.SCHEDULE_DOCTOR_CONFLICT);
        if (schedule.getRoom() != null && schedules.exists(overlap.and((root, query, cb) -> cb.equal(root.get("room").get("id"), schedule.getRoom().getId()))))
            throw new AppException(ErrorCode.SCHEDULE_ROOM_CONFLICT);
    }

    private void generateSlots(DoctorSchedule schedule) {
        LocalTime cursor = schedule.getStartTime();
        while (cursor.isBefore(schedule.getEndTime())) {
            LocalTime end = cursor.plusSeconds(Math.min(1800, Duration.between(cursor, schedule.getEndTime()).getSeconds()));
            schedule.getTimeSlots().add(TimeSlot.builder().doctorSchedule(schedule).startTime(cursor).endTime(end).build());
            cursor = end;
        }
        allocateSlotCapacity(schedule, false);
        synchronizeSlots(schedule);
    }

    /** Balance quotas without recreating slots or reducing existing allocations with booking history. */
    private void allocateSlotCapacity(DoctorSchedule schedule, boolean preserveExisting) {
        List<TimeSlot> slots = schedule.getTimeSlots().stream()
                .sorted(Comparator.comparing(TimeSlot::getStartTime).thenComparing(
                        TimeSlot::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        if (slots.isEmpty()) throw new AppException(ErrorCode.SCHEDULE_CAPACITY_CONFLICT);
        int[] minimums = slots.stream().mapToInt(slot -> preserveExisting
                ? Math.max(slot.getMaxPatients(), slot.getBookedCount()) : slot.getBookedCount()).toArray();
        long minimumTotal = Arrays.stream(minimums).asLongStream().sum();
        int target = schedule.getMaxPatients();
        if (minimumTotal > target) throw new AppException(ErrorCode.SCHEDULE_CAPACITY_CONFLICT);

        // Find the largest common quota that fits. O(slotCount * log(maxPatients)),
        // rather than assigning one patient at a time for potentially large capacities.
        long low = 0, high = target;
        while (low < high) {
            long middle = low + (high - low + 1) / 2;
            long total = 0;
            for (int minimum : minimums) total += Math.max(minimum, middle);
            if (total <= target) low = middle;
            else high = middle - 1;
        }
        long allocated = 0;
        for (int i = 0; i < slots.size(); i++) {
            int quota = Math.max(minimums[i], (int) low);
            slots.get(i).setMaxPatients(quota);
            allocated += quota;
        }
        long remainder = target - allocated;
        for (TimeSlot slot : slots) {
            if (remainder == 0) break;
            if (slot.getMaxPatients() == low) {
                slot.setMaxPatients(slot.getMaxPatients() + 1);
                remainder--;
            }
        }
    }

    private void synchronizeSlots(DoctorSchedule schedule) {
        TimeSlotPolicy.synchronize(schedule, schedule.getTimeSlots());
    }

    private void cancel(DoctorSchedule schedule) {
        if (schedule.getStatus() == EStatusDoctorSchedule.CANCELLED) return;
        schedule.setStatus(EStatusDoctorSchedule.CANCELLED);
        List<Appointment> active = appointments.findByTimeSlotDoctorScheduleIdAndStatusIn(schedule.getId(),
                List.of(EAppointmentStatus.PENDING, EAppointmentStatus.CONFIRMED));
        for (Appointment appointment : active) {
            notifications.save(Notification.builder().user(appointment.getPatient().getUser()).title("Ca khám đã bị hủy")
                    .content("Ca khám ngày " + schedule.getWorkDate() + " đã bị hủy. Vui lòng liên hệ phòng khám để dời lịch " + appointment.getAppointmentCode() + ".")
                    .type("APPOINTMENT_CANCELLED").referenceId(appointment.getId()).build());
            appointment.setStatus(EAppointmentStatus.CANCELLED);
            TimeSlot slot = appointment.getTimeSlot();
            if (slot.getBookedCount() > 0) slot.setBookedCount(slot.getBookedCount() - 1);
        }
        appointments.saveAll(active);
        // Appointment records preserve history; refunds remain the responsibility of payment.
        synchronizeSlots(schedule);
    }

    private int bookedCount(DoctorSchedule schedule) {
        return schedule.getTimeSlots().stream().mapToInt(TimeSlot::getBookedCount).sum();
    }

    private boolean hasHistory(DoctorSchedule schedule) {
        return bookedCount(schedule) > 0 || appointments.existsByTimeSlotDoctorScheduleId(schedule.getId());
    }

    private DoctorSchedule locked(Long id) {
        return schedules.findLockedById(id).orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));
    }

    private Doctor lockDoctor(Long id) {
        return doctors.findLockedById(id).orElseThrow(() -> new AppException(ErrorCode.DOCTOR_NOT_EXISTED));
    }

    private Room resolveRoom(Long id) {
        if (id == null) return null;
        Room room = rooms.findLockedById(id).orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        if (!"AVAILABLE".equals(room.getStatus())) throw new AppException(ErrorCode.ROOM_UNAVAILABLE);
        return room;
    }

    private void requireActive(Doctor doctor) {
        if (doctor.getStatus() != EStatus.ACTIVE) throw new AppException(ErrorCode.DOCTOR_INACTIVE);
    }

    private void validateTime(LocalDate date, LocalTime start, LocalTime end, int capacity) {
        if (date == null || date.isBefore(LocalDate.now()) || start == null || end == null
                || start.getNano() != 0 || end.getNano() != 0 || !start.isBefore(end) || capacity < 1)
            throw new AppException(ErrorCode.SCHEDULE_INVALID_TIME);
    }
}
