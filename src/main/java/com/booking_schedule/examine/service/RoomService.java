package com.booking_schedule.examine.service;

import com.booking_schedule.examine.dto.request.RoomRequest;
import com.booking_schedule.examine.dto.response.RoomResponse;
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
        if (!Set.of("AVAILABLE", "MAINTENANCE", "INACTIVE").contains(status == null ? "" : status))
            throw new AppException(ErrorCode.ROOM_INVALID_REQUEST);
        Room room = locked(id);
        guardClosure(room, status);
        room.setStatus(status);
        return save(room);
    }

    @Transactional
    public String deleteRoom(Long id) {
        Room room = locked(id);
        guardClosure(room, "INACTIVE");
        if (schedules.existsByRoomId(id)) {
            room.setStatus("INACTIVE");
            save(room);
            return "Đã vô hiệu hóa phòng; giữ nguyên lịch sử phân công";
        }
        rooms.delete(room);
        rooms.flush();
        return "Đã xóa phòng chưa có lịch sử phân công";
    }

    public RoomResponse getRoom(Long id) {
        return mapper.toResponse(rooms.findById(id).orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND)));
    }

    public Page<RoomResponse> search(String keyword, Long specialtyId, String status, Pageable pageable) {
        if (status != null) validateStatus(status);
        Specification<Room> filter = filters(keyword, specialtyId, status);
        return rooms.findAll(filter, pageable).map(mapper::toResponse);
    }

    public List<RoomResponse> publicRooms(Long specialtyId) {
        if (specialtyId != null) specialties.findById(specialtyId)
                .orElseThrow(() -> new AppException(ErrorCode.SPECIALTY_NOT_FOUND));
        return rooms.findAll(filters(null, specialtyId, "AVAILABLE"), Sort.by("code", "id"))
                .stream().map(mapper::toResponse).toList();
    }

    private void apply(Room room, RoomRequest request) {
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
            specialty = specialties.findById(request.getSpecialtyId())
                    .orElseThrow(() -> new AppException(ErrorCode.SPECIALTY_NOT_FOUND));
            if (specialty.getStatus() != EStatus.ACTIVE) throw new AppException(ErrorCode.SPECIALTY_INACTIVE);
        }
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
                && schedules.existsByRoomIdAndWorkDateGreaterThanEqualAndStatusIn(room.getId(), LocalDate.now(),
                List.of(EStatusDoctorSchedule.OPEN, EStatusDoctorSchedule.FULL)))
            throw new AppException(ErrorCode.ROOM_HAS_FUTURE_SCHEDULES);
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
                        cb.like(cb.lower(root.get("name")), value, '\\')));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
