package com.booking_schedule.examine.service;

import com.booking_schedule.examine.dto.request.RoomRequest;
import com.booking_schedule.examine.dto.request.RoomUpdateRequest;
import com.booking_schedule.examine.dto.request.RoomAvailabilityRequest;
import com.booking_schedule.examine.dto.response.RoomResponse;
import com.booking_schedule.examine.dto.response.RoomDeleteResponse;
import com.booking_schedule.examine.entity.DoctorSchedule;
import com.booking_schedule.examine.entity.Room;
import com.booking_schedule.examine.entity.Specialty;
import com.booking_schedule.examine.enums.EStatus;
import com.booking_schedule.examine.enums.EStatusDoctorSchedule;
import com.booking_schedule.examine.exception.*;
import com.booking_schedule.examine.mapper.RoomMapper;
import com.booking_schedule.examine.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.JoinType;

import java.time.LocalDate;
import java.sql.SQLException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {
    private static final Set<String> STATUSES = Set.of("AVAILABLE", "OCCUPIED", "MAINTENANCE", "INACTIVE");
    private final RoomRepository rooms;
    private final SpecialtyRepository specialties;
    private final DoctorScheduleRepository schedules;
    private final RoomMapper mapper;

    @Transactional
    public RoomResponse createRoom(RoomRequest request) {
        Room room = new Room();
        apply(room, request);
        return save(room);
    }

    @Transactional
    public RoomResponse updateRoom(Long id, RoomRequest request) {
        Room room = locked(id);
        apply(room, request);
        return save(room);
    }

    @Transactional
    public RoomResponse updateStatus(Long id, String status) {
        validateStatus(status);
        Room room = locked(id);
        guardClosure(room, status);
        room.setStatus(status);
        return save(room);
    }

    @Transactional
    public RoomResponse patchRoom(Long id, RoomUpdateRequest request) {
        if (!request.isSelectionValid()) throw new AppException(ErrorCode.ROOM_INVALID_REQUEST);
        Room room = locked(id);
        Long currentSpecialtyId = room.getSpecialty() == null ? null : room.getSpecialty().getId();
        RoomRequest effective = RoomRequest.builder()
                .code(request.getCode() == null ? room.getCode() : request.getCode())
                .name(request.getName() == null ? room.getName() : request.getName())
                .location(Boolean.TRUE.equals(request.getClearLocation()) ? null
                        : request.getLocation() == null ? room.getLocation() : request.getLocation())
                .specialtyId(Boolean.TRUE.equals(request.getClearSpecialty()) ? null
                        : request.getSpecialtyId() == null ? currentSpecialtyId : request.getSpecialtyId())
                .status(request.getStatus() == null ? room.getStatus() : request.getStatus()).build();
        apply(room, effective, false);
        return save(room);
    }

    @Transactional
    public RoomDeleteResponse deleteRoom(Long id) {
        Room room = locked(id);
        guardClosure(room, "INACTIVE");
        if (schedules.existsByRoomId(id)) {
            room.setStatus("INACTIVE");
            save(room);
            return new RoomDeleteResponse(id, RoomDeleteResponse.Action.DEACTIVATED, "INACTIVE",
                    "Đã vô hiệu hóa phòng; giữ nguyên lịch sử phân công");
        }
        rooms.delete(room);
        rooms.flush();
        return new RoomDeleteResponse(id, RoomDeleteResponse.Action.DELETED, null,
                "Đã xóa phòng chưa có lịch sử phân công");
    }

    public RoomResponse getRoom(Long id) {
        return mapper.toResponse(rooms.findById(id).orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND)));
    }

    public Page<RoomResponse> search(String keyword, Long specialtyId, String status, Pageable pageable) {
        return search(keyword, specialtyId, status, null, pageable);
    }

    public Page<RoomResponse> search(String keyword, Long specialtyId, String status, Boolean sharedOnly, Pageable pageable) {
        validateSharedSelection(specialtyId, sharedOnly);
        if (status != null) validateStatus(status);
        Specification<Room> filter = filters(keyword, specialtyId, status);
        if (Boolean.TRUE.equals(sharedOnly)) filter = filter.and((root, query, cb) -> cb.isNull(root.get("specialty")));
        return rooms.findAll(filter, pageable).map(mapper::toResponse);
    }

    public List<RoomResponse> publicRooms(Long specialtyId) {
        if (specialtyId != null) activeSpecialty(specialtyId);
        return rooms.findAll(eligibleRooms(specialtyId), Sort.by("code", "id"))
                .stream().map(mapper::toResponse).toList();
    }

    public Page<RoomResponse> availableRooms(RoomAvailabilityRequest request, Pageable pageable) {
        if (request.getWorkDate() == null || request.getWorkDate().isBefore(LocalDate.now())
                || request.getStartTime() == null || request.getEndTime() == null || !request.isTimeRangeValid())
            throw new AppException(ErrorCode.ROOM_INVALID_REQUEST);
        validateSharedSelection(request.getSpecialtyId(), request.getSharedOnly());
        if (request.getSpecialtyId() != null) activeSpecialty(request.getSpecialtyId());
        if (request.getExcludeScheduleId() != null && !schedules.existsById(request.getExcludeScheduleId()))
            throw new AppException(ErrorCode.SCHEDULE_NOT_FOUND);
        Specification<Room> spec = eligibleRooms(request.getSpecialtyId())
                .and(filters(request.getKeyword(), null, null));
        if (Boolean.TRUE.equals(request.getSharedOnly()))
            spec = spec.and((root, query, cb) -> cb.isNull(root.get("specialty")));
        spec = spec.and((root, query, cb) -> {
            var conflict = query.subquery(Long.class);
            var schedule = conflict.from(DoctorSchedule.class);
            var predicate = cb.and(cb.equal(schedule.get("room").get("id"), root.get("id")),
                    cb.equal(schedule.get("workDate"), request.getWorkDate()),
                    cb.notEqual(schedule.get("status"), EStatusDoctorSchedule.CANCELLED),
                    cb.lessThan(schedule.get("startTime"), request.getEndTime()),
                    cb.greaterThan(schedule.get("endTime"), request.getStartTime()));
            if (request.getExcludeScheduleId() != null)
                predicate = cb.and(predicate, cb.notEqual(schedule.get("id"), request.getExcludeScheduleId()));
            conflict.select(schedule.get("id")).where(predicate);
            return cb.not(cb.exists(conflict));
        });
        return rooms.findAll(spec, pageable).map(mapper::toResponse);
    }

    private Specification<Room> eligibleRooms(Long specialtyId) {
        return (root, query, cb) -> {
            var specialty = root.join("specialty", JoinType.LEFT);
            var shared = cb.isNull(specialty.get("id"));
            var eligible = cb.equal(specialty.get("status"), EStatus.ACTIVE);
            if (specialtyId != null) eligible = cb.and(eligible, cb.equal(specialty.get("id"), specialtyId));
            return cb.and(cb.equal(root.get("status"), "AVAILABLE"), cb.or(shared, eligible));
        };
    }

    private Specialty activeSpecialty(Long id) {
        Specialty specialty = specialties.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SPECIALTY_NOT_FOUND));
        if (specialty.getStatus() != EStatus.ACTIVE) throw new AppException(ErrorCode.SPECIALTY_INACTIVE);
        return specialty;
    }

    private void validateSharedSelection(Long specialtyId, Boolean sharedOnly) {
        if (Boolean.TRUE.equals(sharedOnly) && specialtyId != null)
            throw new AppException(ErrorCode.ROOM_INVALID_REQUEST);
    }

    private void apply(Room room, RoomRequest request) {
        apply(room, request, true);
    }

    private void apply(Room room, RoomRequest request, boolean validateUnchangedSpecialty) {
        if (request.getCode() == null || request.getCode().isBlank() || request.getCode().length() > 50
                || request.getName() == null || request.getName().isBlank() || request.getName().length() > 255
                || (request.getLocation() != null && request.getLocation().length() > 255))
            throw new AppException(ErrorCode.ROOM_INVALID_REQUEST);
        String code = request.getCode().trim().toUpperCase(Locale.ROOT);
        if (code.length() > 50) throw new AppException(ErrorCode.ROOM_INVALID_REQUEST);
        boolean duplicate = room.getId() == null ? rooms.existsByCodeIgnoreCase(code)
                : rooms.existsByCodeIgnoreCaseAndIdNot(code, room.getId());
        if (duplicate) throw new AppException(ErrorCode.ROOM_CODE_EXISTS);
        Specialty specialty = null;
        if (request.getSpecialtyId() != null) {
            specialty = !validateUnchangedSpecialty && room.getSpecialty() != null
                    && request.getSpecialtyId().equals(room.getSpecialty().getId())
                    ? room.getSpecialty() : activeSpecialty(request.getSpecialtyId());
        }
        Long currentSpecialtyId = room.getSpecialty() == null ? null : room.getSpecialty().getId();
        if (room.getId() != null && !Objects.equals(currentSpecialtyId, request.getSpecialtyId())
                && schedules.existsByRoomIdAndWorkDateGreaterThanEqualAndStatusIn(room.getId(), LocalDate.now(),
                        List.of(EStatusDoctorSchedule.OPEN, EStatusDoctorSchedule.FULL, EStatusDoctorSchedule.CLOSED)))
            throw new AppException(ErrorCode.ROOM_SPECIALTY_CHANGE_NOT_ALLOWED);
        String status = request.getStatus() == null
                ? (room.getId() == null ? "AVAILABLE" : room.getStatus()) : request.getStatus();
        validateStatus(status);
        guardClosure(room, status);
        room.setCode(code);
        room.setName(request.getName().trim());
        room.setLocation(request.getLocation() == null ? null : request.getLocation().trim());
        room.setSpecialty(specialty); // PUT null explicitly restores a shared room.
        room.setStatus(status);
    }

    private void guardClosure(Room room, String status) {
        if (room.getId() != null && ("MAINTENANCE".equals(status) || "INACTIVE".equals(status))
                && hasFutureSchedules(room.getId()))
            throw new AppException(ErrorCode.ROOM_HAS_FUTURE_SCHEDULES);
    }

    private boolean hasFutureSchedules(Long roomId) {
        return schedules.existsByRoomIdAndWorkDateGreaterThanEqualAndStatusIn(roomId, LocalDate.now(),
                List.of(EStatusDoctorSchedule.OPEN, EStatusDoctorSchedule.FULL));
    }

    private void validateStatus(String status) {
        if (status == null || !STATUSES.contains(status)) throw new AppException(ErrorCode.ROOM_INVALID_REQUEST);
    }

    private Room locked(Long id) {
        // The same room lock is held by schedule assignment until commit.
        return rooms.findLockedById(id).orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
    }

    private RoomResponse save(Room room) {
        try {
            return mapper.toResponse(rooms.saveAndFlush(room));
        } catch (DataIntegrityViolationException exception) {
            // code is Room's only client-supplied unique field. MySQL's generated
            // constraint name need not contain "code", so use the duplicate-key error.
            Throwable cause = exception.getMostSpecificCause();
            if (cause instanceof SQLException sql
                    && (sql.getErrorCode() == 1062 || "23505".equals(sql.getSQLState())))
                throw new AppException(ErrorCode.ROOM_CODE_EXISTS);
            throw exception;
        }
    }

    private Specification<Room> filters(String keyword, Long specialtyId, String status) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (specialtyId != null) predicates.add(cb.equal(root.get("specialty").get("id"), specialtyId));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (keyword != null && !keyword.isBlank()) {
                String value = "%" + keyword.trim().toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("code")), value, '\\'),
                        cb.like(cb.lower(root.get("name")), value, '\\'),
                        cb.like(cb.lower(root.get("location")), value, '\\')));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
