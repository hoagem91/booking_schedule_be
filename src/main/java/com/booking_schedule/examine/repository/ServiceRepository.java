package com.booking_schedule.examine.repository;

import com.booking_schedule.examine.entity.Service;
import com.booking_schedule.examine.enums.EStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long>, JpaSpecificationExecutor<Service> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from Service s where s.id = :id")
    Optional<Service> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);

    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
    Optional<Service> findByCode(String code);
    List<Service> findBySpecialtyId(Long specialtyId);
    List<Service> findByStatus(EStatus status);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    boolean existsBySpecialtyId(Long specialtyId);
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
}
