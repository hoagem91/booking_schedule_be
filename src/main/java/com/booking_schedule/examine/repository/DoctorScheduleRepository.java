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
    @Query("select s.doctor.id from DoctorSchedule s where s.id = :id")
    Optional<Long> findDoctorIdByScheduleId(@Param("id") Long id);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from DoctorSchedule s where s.doctor.id = :doctorId "
            + "and (s.workDate > :date or (s.workDate = :date and s.endTime > :time)) "
            + "and s.status in :statuses order by s.id")
    List<DoctorSchedule> findLockedUnfinished(@Param("doctorId") Long doctorId, @Param("date") LocalDate date,
            @Param("time") LocalTime time, @Param("statuses") List<EStatusDoctorSchedule> statuses);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from DoctorSchedule s where s.id = :id")
    Optional<DoctorSchedule> findLockedById(@Param("id") Long id);
    List<DoctorSchedule> findByDoctorId(Long doctorId);
    List<DoctorSchedule> findByDoctorIdAndWorkDate(Long doctorId, LocalDate workDate);
    boolean existsByDoctorId(Long doctorId);
    boolean existsByRoomId(Long roomId);
    @Query("select (count(s) > 0) from DoctorSchedule s left join s.room r "
            + "where s.workDate >= :date and s.status in :statuses "
            + "and (s.doctor.specialty.id = :specialtyId or r.specialty.id = :specialtyId)")
    boolean existsActiveSchedulesForSpecialty(@Param("specialtyId") Long specialtyId,
            @Param("date") LocalDate date, @Param("statuses") List<EStatusDoctorSchedule> statuses);
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
