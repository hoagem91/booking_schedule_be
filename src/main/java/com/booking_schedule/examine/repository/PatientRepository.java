package com.booking_schedule.examine.repository;

import com.booking_schedule.examine.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Patient> {
    @Query("select p.user.id from Patient p where p.id = :id")
    Optional<Long> findUserIdByPatientId(@Param("id") Long id);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Patient p where p.user.id = :userId")
    Optional<Patient> findLockedByUserId(@Param("userId") Long userId);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Patient p where p.id = :id")
    Optional<Patient> findLockedById(@Param("id") Long id);
    Optional<Patient> findByPatientCode(String patientCode);

    // Tìm bệnh nhân theo ID của User (user_id là Long, khớp với User.id)
    Optional<Patient> findByUserId(Long userId);

    boolean existsByPatientCode(String patientCode);

    boolean existsByUserId(Long userId);
}
