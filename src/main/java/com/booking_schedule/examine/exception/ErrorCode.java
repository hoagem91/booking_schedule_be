package com.booking_schedule.examine.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "User existed", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1003, "Username must be at least {min} characters", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1004, "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1005, "User not existed", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),
    INVALID_DOB(1008, "Your age must be at least {min}", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL(1009, "Invalid email address", HttpStatus.BAD_REQUEST),
    EMAIL_IS_REQUIRED(1010, "Email is required", HttpStatus.BAD_REQUEST),
    //    Role
    INVALID_ROLE(1011, "Invalid Role", HttpStatus.BAD_REQUEST),
    USER_INACTIVE(1012, "User account is inactive", HttpStatus.FORBIDDEN),
    ROLE_PROFILE_CONFLICT(1013, "Roles must match existing patient and doctor profiles", HttpStatus.CONFLICT),
    USER_SELF_DEACTIVATION_NOT_ALLOWED(1014, "You cannot deactivate your own account", HttpStatus.CONFLICT),
    USER_LAST_SUPER_ADMIN(1015, "The last active SUPER_ADMIN must remain active and retain its role", HttpStatus.CONFLICT),
    USER_SELF_ROLE_CHANGE_NOT_ALLOWED(1016, "You cannot remove all administrative roles from your own account", HttpStatus.CONFLICT),
    USER_CURRENT_PASSWORD_REQUIRED(1017, "Current password is required", HttpStatus.BAD_REQUEST),
    USER_CURRENT_PASSWORD_INVALID(1018, "Current password is incorrect", HttpStatus.BAD_REQUEST),
    USERNAME_EXISTED(1019, "Username already exists", HttpStatus.CONFLICT),
    USER_EMAIL_EXISTED(1020, "Email already exists", HttpStatus.CONFLICT),
    USER_INVALID_REQUEST(1021, "Invalid user data or status", HttpStatus.BAD_REQUEST),
    USER_SELF_PASSWORD_RESET_NOT_ALLOWED(1022, "Use the personal change-password endpoint for your own password", HttpStatus.CONFLICT),
    PATIENT_DELETE_NOT_ALLOWED(2005, "Patient profiles are retained; deactivate the user account instead", HttpStatus.CONFLICT),

    //    Patient
    PATIENT_ALREADY_EXISTS(2001, "Patient profile already exists for this user", HttpStatus.BAD_REQUEST),
    USER_IS_NOT_PATIENT(2002, "User does not have PATIENT role", HttpStatus.FORBIDDEN),
    PATIENT_NOT_FOUND(2003, "Patient not found", HttpStatus.NOT_FOUND),
    PATIENT_CODE_EXISTS(2004, "Patient code already exists", HttpStatus.BAD_REQUEST),
    PATIENT_INVALID_REQUEST(2006, "Invalid patient data or account selection", HttpStatus.BAD_REQUEST),

    //    Specialty
    SPECIALTY_NOT_FOUND(3001, "Specialty not found", HttpStatus.NOT_FOUND),
    SPECIALTY_EXISTED(3002, "Specialty already exists", HttpStatus.BAD_REQUEST),
    SPECIALTY_NOT_EXISTED(3003, "Specialty does not exist", HttpStatus.NOT_FOUND),
    SPECIALTY_INACTIVE(3004, "Specialty is not active", HttpStatus.BAD_REQUEST),
    SPECIALTY_ALREADY_INACTIVE(3005, "Specialty is already inactive", HttpStatus.BAD_REQUEST),
    SPECIALTY_CODE_EXISTED(3006, "Specialty code already exists", HttpStatus.BAD_REQUEST),
    SPECIALTY_NAME_EXISTED(3007, "Specialty name already exists (case-insensitive)", HttpStatus.BAD_REQUEST),
    SPECIALTY_HAS_FUTURE_SCHEDULES(3008, "Cancel or move OPEN/FULL schedules from today onward before deactivating the specialty", HttpStatus.CONFLICT),
    SPECIALTY_INVALID_REQUEST(3009, "Invalid specialty data or status", HttpStatus.BAD_REQUEST),

    //    Doctor
    DOCTOR_ALREADY_EXISTS(4001, "Doctor profile already exists for this user", HttpStatus.BAD_REQUEST),
    DOCTOR_NOT_EXISTED(4002, "Doctor not existed", HttpStatus.NOT_FOUND),
    USER_IS_NOT_DOCTOR(4003, "User does not have DOCTOR role", HttpStatus.FORBIDDEN),
    DOCTOR_NOT_FOUND(4004, "Doctor not found", HttpStatus.NOT_FOUND),
    DOCTOR_HAS_HISTORY(4005, "Cannot hard-delete doctor with existing schedules or appointments", HttpStatus.CONFLICT),
    DOCTOR_ALREADY_INACTIVE(4006, "Doctor is already inactive", HttpStatus.BAD_REQUEST),

    //    Service
    SERVICE_EXISTED(5001, "Service already exists", HttpStatus.BAD_REQUEST),
    SERVICE_NOT_EXISTED(5003, "Service not existed", HttpStatus.NOT_FOUND),
    SERVICE_CODE_EXISTED(5002, "Service code already exists", HttpStatus.BAD_REQUEST),
    SERVICE_NAME_EXISTED(5004, "Service name already existed", HttpStatus.BAD_REQUEST),

    // Doctor-schedule
    SERVICE_ALREADY_INACTIVE(5005, "Service is already inactive", HttpStatus.BAD_REQUEST),
    SERVICE_INVALID_REQUEST(5006, "Invalid service data or status", HttpStatus.BAD_REQUEST),
    DOCTOR_INACTIVE(6001, "Doctor is not active", HttpStatus.BAD_REQUEST),
    ROOM_NOT_FOUND(6002, "Room not found", HttpStatus.NOT_FOUND),
    ROOM_UNAVAILABLE(6003, "Room is not available", HttpStatus.BAD_REQUEST),
    SCHEDULE_NOT_FOUND(6004, "Doctor schedule not found", HttpStatus.NOT_FOUND),
    SCHEDULE_INVALID_TIME(6005, "Invalid schedule date, time or capacity", HttpStatus.BAD_REQUEST),
    SCHEDULE_DOCTOR_CONFLICT(6006, "Doctor has an overlapping schedule", HttpStatus.CONFLICT),
    SCHEDULE_ROOM_CONFLICT(6007, "Room has an overlapping schedule", HttpStatus.CONFLICT),
    SCHEDULE_HAS_BOOKINGS(6008, "Booked schedule permits only room changes or capacity increases", HttpStatus.CONFLICT),
    SCHEDULE_INVALID_STATUS(6009, "Cancelled schedule cannot be reopened", HttpStatus.CONFLICT),
    SCHEDULE_CAPACITY_CONFLICT(6010, "Schedule capacity cannot preserve existing slot allocations", HttpStatus.CONFLICT),
    SCHEDULE_DOCTOR_MISMATCH(6011, "doctorId does not match the doctor assigned to this schedule", HttpStatus.CONFLICT),
    TIME_SLOT_NOT_FOUND(7001, "Time slot not found", HttpStatus.NOT_FOUND),
    TIME_SLOT_HAS_BOOKINGS(7002, "Cannot close a time slot with active bookings", HttpStatus.CONFLICT),
    TIME_SLOT_NOT_AVAILABLE(7003, "Time slot or schedule is not available", HttpStatus.CONFLICT),
    TIME_SLOT_INVALID_STATUS(7004, "Only AVAILABLE, CLOSED or UNAVAILABLE may be requested", HttpStatus.BAD_REQUEST),
    TIME_SLOT_INVALID_COUNT(7005, "Cannot release a time slot with no booked patients", HttpStatus.CONFLICT),
    APPOINTMENT_NOT_FOUND(8001, "Appointment not found", HttpStatus.NOT_FOUND),
    APPOINTMENT_INVALID_REQUEST(8002, "Invalid appointment data or date range", HttpStatus.BAD_REQUEST),
    APPOINTMENT_SLOT_MISMATCH(8003, "Slot does not match doctor or appointment date", HttpStatus.CONFLICT),
    APPOINTMENT_DUPLICATE(8004, "Patient has an overlapping appointment", HttpStatus.CONFLICT),
    APPOINTMENT_INVALID_TRANSITION(8005, "Appointment status transition is not allowed", HttpStatus.CONFLICT),
    APPOINTMENT_CANCEL_TOO_LATE(8006, "Cancellation deadline has passed", HttpStatus.CONFLICT),
    SERVICE_INACTIVE(8007, "Service is not active", HttpStatus.BAD_REQUEST),
    APPOINTMENT_SPECIALTY_MISMATCH(8008, "Service does not belong to doctor's specialty", HttpStatus.BAD_REQUEST),
    PROMOTION_NOT_FOUND(8009, "Promotion not found", HttpStatus.NOT_FOUND),
    PROMOTION_INVALID(8010, "Promotion is inactive, expired, exhausted or invalid", HttpStatus.CONFLICT),
    APPOINTMENT_PRICE_UNAVAILABLE(8011, "Historical appointment price is unavailable", HttpStatus.CONFLICT),
    APPOINTMENT_INVALID_SETTING(8012, "CANCEL_BEFORE_HOURS must be a nonnegative integer", HttpStatus.CONFLICT),
    APPOINTMENT_TOO_EARLY(8013, "Appointment has not started or ended yet", HttpStatus.CONFLICT),
    REVIEW_NOT_ALLOWED(8014, "Only a completed appointment can be reviewed once", HttpStatus.CONFLICT),
    ROOM_CODE_EXISTS(9001, "Room code already exists", HttpStatus.CONFLICT),
    ROOM_INVALID_REQUEST(9002, "Invalid room data or status", HttpStatus.BAD_REQUEST),
    ROOM_HAS_FUTURE_SCHEDULES(9003, "Cancel or move OPEN/FULL schedules from today onward before closing the room", HttpStatus.CONFLICT),
    ROOM_SPECIALTY_CHANGE_NOT_ALLOWED(9004, "Cancel or move non-cancelled schedules from today onward before changing the room specialty", HttpStatus.CONFLICT),
    ;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}
