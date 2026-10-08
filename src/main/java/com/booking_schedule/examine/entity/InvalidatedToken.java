package com.booking_schedule.examine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "invalidated_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvalidatedToken {

    @Id
    @Column(name = "id", nullable = false, unique = true)
    private String id; // JWT ID (jti)

    @Column(name = "expiry_time", nullable = false)
    private Date expiryTime;
}
