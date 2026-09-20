package com.booking_schedule.examine.repository;

import com.booking_schedule.examine.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByDoctorCode(String doctorCode);

    // Tìm bác sĩ theo ID của bảng User (khi bác sĩ đang đăng nhập và muốn lấy thông tin hồ sơ)
    Optional<Doctor> findByUserId(Long userId);

    // Lấy danh sách bác sĩ theo chuyên khoa
    List<Doctor> findBySpecialtyId(Long specialtyId);
}
