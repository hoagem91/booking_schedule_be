package com.booking_schedule.examine.repository;

import com.booking_schedule.examine.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByAppointmentId(Long appointmentId);
    List<Review> findByDoctorIdAndStatus(Long doctorId, String status); // Tìm review của bác sĩ đang ở trạng thái VISIBLE
}
