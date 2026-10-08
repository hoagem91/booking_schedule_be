package com.booking_schedule.examine.repository;

import com.booking_schedule.examine.entity.Doctor;
import com.booking_schedule.examine.enums.EStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select d from Doctor d where d.id = :id")
    Optional<Doctor> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);
    Optional<Doctor> findByDoctorCode(String doctorCode);

    // Tìm bác sĩ theo ID của bảng User (quan hệ 1-1)
    Optional<Doctor> findByUserId(Long userId);

    // Kiểm tra user đã có hồ sơ bác sĩ chưa (tránh vi phạm UNIQUE user_id)
    boolean existsByUserId(Long userId);

    // Kiểm tra doctorCode đã tồn tại chưa (tránh vi phạm UNIQUE doctor_code)
    boolean existsByDoctorCode(String doctorCode);

    // Kiểm tra doctorCode trùng với bác sĩ khác (dùng khi update)
    boolean existsByDoctorCodeAndIdNot(String doctorCode, Long id);

    boolean existsBySpecialtyId(Long specialtyId);

    // Lấy danh sách bác sĩ theo chuyên khoa
    List<Doctor> findBySpecialtyId(Long specialtyId);

    // Lấy danh sách bác sĩ theo status
    List<Doctor> findByStatus(EStatus status);

    // Lọc theo chuyên khoa + status
    List<Doctor> findBySpecialtyIdAndStatus(Long specialtyId, EStatus status);
}
