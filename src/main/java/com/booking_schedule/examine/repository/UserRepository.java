package com.booking_schedule.examine.repository;

import com.booking_schedule.examine.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    // Rất hữu ích khi viết chức năng Đăng ký (Register)
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

}
