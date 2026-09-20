package com.booking_schedule.examine.repository;

import com.booking_schedule.examine.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByPatientCode(String patientCode);

    // Tìm bệnh nhân theo ID của User (khi user đăng nhập và vào xem hồ sơ bệnh án)
    Optional<Patient> findByUserId(Long userId);
}