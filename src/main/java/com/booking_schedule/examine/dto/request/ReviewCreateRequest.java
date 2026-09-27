package com.booking_schedule.examine.dto.request;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewCreateRequest {
    @NotNull(message = "ID cuộc hẹn (appointmentId) không được để trống")
    private Long appointmentId;

    @NotNull(message = "Điểm đánh giá không được để trống")
    @Min(value = 1, message = "Đánh giá thấp nhất là 1 sao")
    @Max(value = 5, message = "Đánh giá cao nhất là 5 sao")
    private Integer rating;

    @Size(max = 2000, message = "Nội dung đánh giá không vượt quá 2000 ký tự")
    private String comment;
}
