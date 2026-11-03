package com.booking_schedule.examine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "doctor_audits", indexes = @Index(name = "idx_doctor_audit_target", columnList = "doctor_id,id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DoctorAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "actor_user_id", nullable = false, updatable = false)
    private Long actorUserId;
    @Column(name = "doctor_id", nullable = false, updatable = false)
    private Long doctorId;
    @Column(length = 32, nullable = false, updatable = false)
    private String action;
    // Only field names, never description/contact values or full request snapshots.
    @Column(name = "changed_fields", length = 500, nullable = false, updatable = false)
    private String changedFields;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

