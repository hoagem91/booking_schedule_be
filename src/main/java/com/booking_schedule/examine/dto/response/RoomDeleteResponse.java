package com.booking_schedule.examine.dto.response;

public record RoomDeleteResponse(Long id, Action action, String status, String message) {
    public enum Action { DELETED, DEACTIVATED }
}
