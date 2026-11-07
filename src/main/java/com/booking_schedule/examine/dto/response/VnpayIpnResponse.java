package com.booking_schedule.examine.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class VnpayIpnResponse {
    private String rspCode;
    private String message;
}
