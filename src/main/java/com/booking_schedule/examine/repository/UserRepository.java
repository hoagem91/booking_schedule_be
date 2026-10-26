package com.booking_schedule.examine.repository;

import com.booking_schedule.examine.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<User> {
    Optional<User> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    long countByStatusAndRolesName(String status, com.booking_schedule.examine.enums.ERole role);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where u.id = :id")
    Optional<User> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    // Rất hữu ích khi viết chức năng Đăng ký (Register)
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

}
