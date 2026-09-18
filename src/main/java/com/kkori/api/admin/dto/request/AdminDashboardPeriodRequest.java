package com.kkori.api.admin.dto.request;

import com.kkori.api.admin.exception.AdminDashboardInvalidPeriodException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public record AdminDashboardPeriodRequest(LocalDate from, LocalDate to, DashboardUnit unit) {

    public static AdminDashboardPeriodRequest of(String from, String to, String unit) {
        LocalDate parsedFrom = parseDate(from, "from");
        LocalDate parsedTo = parseDate(to, "to");
        if (parsedFrom.isAfter(parsedTo)) {
            throw new AdminDashboardInvalidPeriodException("from must not be after to");
        }
        return new AdminDashboardPeriodRequest(parsedFrom, parsedTo, parseUnit(unit));
    }

    private static LocalDate parseDate(String value, String field) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new AdminDashboardInvalidPeriodException(field + " must be a valid date (yyyy-MM-dd)");
        }
    }

    private static DashboardUnit parseUnit(String value) {
        try {
            return DashboardUnit.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new AdminDashboardInvalidPeriodException("unit must be one of DAY, WEEK, MONTH");
        }
    }
}
