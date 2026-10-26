package com.booking_schedule.examine.service;

import com.booking_schedule.examine.exception.*;
import com.booking_schedule.examine.repository.UserRepository;
import java.util.Locale;

/** Passwords are never normalized. */
public final class UserIdentitySupport {
    private UserIdentitySupport() {}
    public static String username(String value) { return value == null ? null : value.trim().toLowerCase(Locale.ROOT); }
    public static String email(String value) { return value == null ? null : value.trim().toLowerCase(Locale.ROOT); }
    public static String text(String value) { return value == null ? null : value.trim(); }
    public static void checkAvailable(UserRepository users, Long id, String username, String email) {
        if (id == null && users.existsByUsernameIgnoreCase(username)) throw new AppException(ErrorCode.USERNAME_EXISTED);
        if (email != null && (id == null ? users.existsByEmailIgnoreCase(email) : users.existsByEmailIgnoreCaseAndIdNot(email, id)))
            throw new AppException(ErrorCode.USER_EMAIL_EXISTED);
    }
    public static AppException conflict(org.springframework.dao.DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation && violation.getConstraintName() != null) {
                String name = violation.getConstraintName().toLowerCase(Locale.ROOT);
                if (name.contains("username")) return new AppException(ErrorCode.USERNAME_EXISTED);
                if (name.contains("email")) return new AppException(ErrorCode.USER_EMAIL_EXISTED);
            }
        }
        return new AppException(ErrorCode.USER_EXISTED);
    }
}
