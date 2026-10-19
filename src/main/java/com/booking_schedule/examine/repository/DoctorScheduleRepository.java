package com.booking_schedule.examine.repository;

import com.booking_schedule.examine.entity.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import com.booking_schedule.examine.enums.EStatusDoctorSchedule;
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
    boolean existsByRoomId(Long roomId);
    boolean existsByRoomIdAndWorkDateGreaterThanEqualAndStatusIn(Long roomId, LocalDate date,
                                                               List<EStatusDoctorSchedule> statuses);

    // Locking reads see committed assignments even with MySQL REPEATABLE_READ snapshots.
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from DoctorSchedule s where s.workDate = :date and s.status <> :cancelled "
            + "and s.startTime < :end and s.endTime > :start and (:id is null or s.id <> :id) "
            + "and (s.doctor.id = :doctorId or (:roomId is not null and s.room.id = :roomId)) order by s.id")
    List<DoctorSchedule> findLockedConflicts(@Param("date") LocalDate date, @Param("start") LocalTime start,
            @Param("end") LocalTime end, @Param("id") Long id, @Param("doctorId") Long doctorId,
            @Param("roomId") Long roomId, @Param("cancelled") EStatusDoctorSchedule cancelled);
}
