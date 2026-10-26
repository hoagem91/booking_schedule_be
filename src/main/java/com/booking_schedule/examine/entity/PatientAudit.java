package com.booking_schedule.examine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "patient_audits", indexes = @Index(name = "idx_patient_audit_target", columnList = "patient_id,id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PatientAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "actor_user_id", nullable = false, updatable = false)
    private Long actorUserId;
    @Column(name = "patient_id", nullable = false, updatable = false)
    private Long patientId;
    @Column(length = 32, nullable = false, updatable = false)
    private String action;
    // Only field names, never DOB/address/insurance/contact values or full request snapshots.
    @Column(name = "changed_fields", length = 500, nullable = false, updatable = false)
    private String changedFields;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
