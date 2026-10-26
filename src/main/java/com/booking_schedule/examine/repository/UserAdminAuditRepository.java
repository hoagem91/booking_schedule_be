package com.booking_schedule.examine.repository;

import com.booking_schedule.examine.entity.UserAdminAudit;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAdminAuditRepository extends JpaRepository<UserAdminAudit, Long> {
    Page<UserAdminAudit> findByTargetUserId(Long targetUserId, Pageable pageable);
}
