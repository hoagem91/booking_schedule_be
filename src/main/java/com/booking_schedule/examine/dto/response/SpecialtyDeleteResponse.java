package com.booking_schedule.examine.dto.response;

import com.booking_schedule.examine.enums.EStatus;

public record SpecialtyDeleteResponse(Long id, Action action, EStatus status, String message) {
    public enum Action { DELETED, DEACTIVATED }
}
