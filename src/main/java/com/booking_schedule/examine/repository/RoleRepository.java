package com.booking_schedule.examine.repository;

import com.booking_schedule.examine.entity.Role;
import com.booking_schedule.examine.enums.ERole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    // A stable row serializes admin mutations, including last-SUPER_ADMIN checks.
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from Role r where r.name = :name")
    Optional<Role> findLockedByName(@org.springframework.data.repository.query.Param("name") ERole name);
    Optional<Role> findByName(ERole name);
    List<Role> findByNameIn(Collection<ERole> names);
}
