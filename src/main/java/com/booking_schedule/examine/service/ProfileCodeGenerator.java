package com.booking_schedule.examine.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class ProfileCodeGenerator {
    private ProfileCodeGenerator() {}
    public static String generate(String prefix) {
        return prefix + "-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + UUID.randomUUID().toString().replace("-", "");
    }
}
