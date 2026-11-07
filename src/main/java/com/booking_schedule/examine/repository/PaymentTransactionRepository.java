package com.booking_schedule.examine.repository;

import com.booking_schedule.examine.entity.PaymentTransaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByTxnRef(String txnRef);
    boolean existsByTxnRef(String txnRef);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from PaymentTransaction t join fetch t.payment p join fetch p.appointment where t.txnRef = :txnRef")
    Optional<PaymentTransaction> findLockedByTxnRef(@Param("txnRef") String txnRef);
}
