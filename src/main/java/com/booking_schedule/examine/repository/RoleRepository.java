package com.booking_schedule.examine.repository;

import com.booking_schedule.examine.entity.Role;
import com.booking_schedule.examine.enums.ERole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    // Truy vấn theo Enum ERole
    Optional<Role> findByName(ERole name);
}
