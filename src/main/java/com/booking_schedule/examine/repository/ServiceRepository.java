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
    Optional<Service> findByCode(String code);
    List<Service> findBySpecialtyId(Long specialtyId);
    List<Service> findByStatus(EStatus status);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    boolean existsBySpecialtyId(Long specialtyId);
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
}
