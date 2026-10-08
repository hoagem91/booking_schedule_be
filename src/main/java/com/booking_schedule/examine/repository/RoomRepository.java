package com.booking_schedule.examine.repository;

import com.booking_schedule.examine.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from Room r where r.id = :id")
    Optional<Room> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);
    Optional<Room> findByCode(String code);

    boolean existsBySpecialtyId(Long specialtyId);
}
