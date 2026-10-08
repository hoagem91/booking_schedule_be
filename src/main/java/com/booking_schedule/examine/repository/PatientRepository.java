package com.booking_schedule.examine.repository;

import com.booking_schedule.examine.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByPatientCode(String patientCode);

    // Tìm bệnh nhân theo ID của User (user_id là Long, khớp với User.id)
    Optional<Patient> findByUserId(Long userId);

    boolean existsByPatientCode(String patientCode);

    boolean existsByUserId(Long userId);
}