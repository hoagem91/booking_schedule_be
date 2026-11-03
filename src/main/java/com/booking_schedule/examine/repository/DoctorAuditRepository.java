package com.booking_schedule.examine.repository;
import com.booking_schedule.examine.entity.DoctorAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
public interface DoctorAuditRepository extends JpaRepository<DoctorAudit, Long> {
    Page<DoctorAudit> findByDoctorId(Long doctorId, Pageable pageable);
}
