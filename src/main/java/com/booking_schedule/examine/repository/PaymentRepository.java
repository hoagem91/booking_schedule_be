package com.booking_schedule.examine.repository;

import com.booking_schedule.examine.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Payment p where p.id = :id")
    Optional<Payment> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);
    Optional<Payment> findByPaymentCode(String paymentCode);
    Optional<Payment> findByAppointmentId(Long appointmentId);
}