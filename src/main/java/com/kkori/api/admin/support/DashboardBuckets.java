package com.kkori.api.admin.support;

import com.kkori.api.admin.dto.request.DashboardUnit;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

/** from~to(KST 날짜)를 unit(DAY/WEEK/MONTH) 버킷 시작일 목록으로 나눈다. WEEK는 KST 월요일 시작. */
public final class DashboardBuckets {

    private DashboardBuckets() {
    }

    public static List<LocalDate> bucketStarts(LocalDate from, LocalDate to, DashboardUnit unit) {
        List<LocalDate> starts = new ArrayList<>();
        LocalDate cursor = bucketStartOf(from, unit);
        LocalDate limit = bucketStartOf(to, unit);
        while (!cursor.isAfter(limit)) {
            starts.add(cursor);
            cursor = next(cursor, unit);
        }
        return starts;
    }

    public static LocalDate bucketStartOf(LocalDate date, DashboardUnit unit) {
        return switch (unit) {
            case DAY -> date;
            case WEEK -> KstClock.mondayOfWeek(date);
            case MONTH -> date.withDayOfMonth(1);
        };
    }

    /** bucketStart가 속한 버킷의 마지막 날짜. hardCap을 넘지 않도록 자른다(조회 기간 to). */
    public static LocalDate bucketEnd(LocalDate bucketStart, DashboardUnit unit, LocalDate hardCap) {
        LocalDate end = switch (unit) {
            case DAY -> bucketStart;
            case WEEK -> bucketStart.plusDays(6);
            case MONTH -> bucketStart.plusMonths(1).minusDays(1);
        };
        return end.isAfter(hardCap) ? hardCap : end;
    }

    private static LocalDate next(LocalDate bucketStart, DashboardUnit unit) {
        return switch (unit) {
            case DAY -> bucketStart.plusDays(1);
            case WEEK -> bucketStart.plusWeeks(1);
            case MONTH -> bucketStart.plusMonths(1);
        };
    }
}
