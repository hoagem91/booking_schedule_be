package com.booking_schedule.examine.repository;

import com.booking_schedule.examine.entity.PatientAudit;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientAuditRepository extends JpaRepository<PatientAudit, Long> {
    Page<PatientAudit> findByPatientId(Long patientId, Pageable pageable);
}
