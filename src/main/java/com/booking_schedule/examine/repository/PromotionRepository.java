package com.booking_schedule.examine.repository;

import com.booking_schedule.examine.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Promotion p where p.id = :id")
    Optional<Promotion> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);
    Optional<Promotion> findByCode(String code);
    List<Promotion> findByStatus(String status);
}
