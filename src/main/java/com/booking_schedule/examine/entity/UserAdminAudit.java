package com.booking_schedule.examine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_admin_audits", indexes = @Index(name = "idx_user_audit_target", columnList = "target_user_id,id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserAdminAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "actor_user_id", nullable = false, updatable = false)
    private Long actorUserId;
    @Column(name = "target_user_id", nullable = false, updatable = false)
    private Long targetUserId;
    @Column(nullable = false, length = 40, updatable = false)
    private String action;
    @Column(name = "password_changed", nullable = false, updatable = false)
    private boolean passwordChanged;
    // Explicit allowlisted JSON snapshots. Never serialize User or request objects.
    @Lob @Column(name = "before_state", columnDefinition = "TEXT", updatable = false)
    private String beforeState;
    @Lob @Column(name = "after_state", columnDefinition = "TEXT", nullable = false, updatable = false)
    private String afterState;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
