package com.booking_schedule.examine.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {

    @Pattern(regexp = "^.*\\S.*$", message = "Mật khẩu không được chỉ chứa khoảng trắng")
    String password;

    @Size(max = 100, message = "Họ và tên không vượt quá 100 ký tự")
    @Pattern(regexp = "^.*\\S.*$", message = "Họ và tên không được chỉ chứa khoảng trắng")
    private String fullName;

    @Email(message = "Email không đúng định dạng")
    @Pattern(regexp = "^.*\\S.*$", message = "Email không được chỉ chứa khoảng trắng")
    private String email;

    @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Số điện thoại không đúng định dạng Việt Nam")
    private String phone;

    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Trạng thái chỉ được là ACTIVE hoặc INACTIVE")
    private String status;

    Set<String> roles;
}
