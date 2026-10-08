package com.booking_schedule.examine.repository;

import com.booking_schedule.examine.entity.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long>, JpaSpecificationExecutor<DoctorSchedule> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from DoctorSchedule s where s.id = :id")
    Optional<DoctorSchedule> findLockedById(@Param("id") Long id);
    List<DoctorSchedule> findByDoctorId(Long doctorId);
    List<DoctorSchedule> findByDoctorIdAndWorkDate(Long doctorId, LocalDate workDate);
    boolean existsByDoctorId(Long doctorId);
}
