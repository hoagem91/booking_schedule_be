package com.booking_schedule.examine.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {
    public void setUsername(String value) { this.username = value == null ? null : value.trim().toLowerCase(java.util.Locale.ROOT); }
    public void setEmail(String value) { this.email = value == null ? null : value.trim().toLowerCase(java.util.Locale.ROOT); }
    public void setFullName(String value) { this.fullName = value == null ? null : value.trim(); }
    public void setPhone(String value) { this.phone = value == null ? null : value.trim(); }
    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 4, max = 100, message = "Tên đăng nhập phải từ 4 đến 100 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9_.]+$", message = "Tên đăng nhập chỉ chứa chữ cái, số, dấu gạch dưới hoặc dấu chấm")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 100, message = "Mật khẩu phải chứa ít nhất 6 ký tự")
    private String password;

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 100, message = "Họ và tên không vượt quá 100 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Số điện thoại không đúng định dạng Việt Nam")
    private String phone;
}
