package com.booking_schedule.examine.entity;

import jakarta.persistence.*;
import com.booking_schedule.examine.enums.ETimeSlotStatus;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "time_slots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private DoctorSchedule doctorSchedule;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Builder.Default
    @Column(name = "max_patients")
    private Integer maxPatients = 1;

    @Builder.Default
    @Column(name = "booked_count")
    private Integer bookedCount = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    private ETimeSlotStatus status = ETimeSlotStatus.AVAILABLE;

    // Keep an individual admin closure when the parent schedule is reopened or resized.
    @Builder.Default
    @Column(name = "manually_closed", nullable = false, columnDefinition = "boolean default false")
    private boolean manuallyClosed = false;
}
