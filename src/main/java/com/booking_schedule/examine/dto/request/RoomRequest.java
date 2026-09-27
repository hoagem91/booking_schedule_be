package com.booking_schedule.examine.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomRequest {
    @NotBlank(message = "Mã phòng không được để trống")
    @Size(max = 50, message = "Mã phòng không được vượt quá 50 ký tự")
    private String code;

    @NotBlank(message = "Tên phòng không được để trống")
    @Size(max = 255, message = "Tên phòng không được vượt quá 255 ký tự")
    private String name;

    @Size(max = 255, message = "Vị trí không được vượt quá 255 ký tự")
    private String location;

    // Tùy chọn: Có thể gán phòng thuộc về một chuyên khoa cụ thể hoặc để null (phòng đa năng)
    private Long specialtyId;

    @Size(max = 30, message = "Trạng thái không được vượt quá 30 ký tự")
    private String status; // AVAILABLE, MAINTENANCE, INACTIVE...
}
