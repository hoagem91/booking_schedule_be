package com.booking_schedule.examine.repository;

import com.booking_schedule.examine.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {
    Optional<Service> findByCode(String code);
    List<Service> findBySpecialtyId(Long specialtyId);
}