package com.kkori.api.admin.support;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class KstClockTest {

    @Test
    void kstDateOfStaysOnSameUtcCalendarDayBeforeMidnightBoundary() {
        // UTC 14:59:59 -> KST 23:59:59 (같은 날)
        LocalDateTime storedUtcNaive = LocalDateTime.of(2026, 1, 1, 14, 59, 59);

        assertThat(KstClock.kstDateOf(storedUtcNaive)).isEqualTo(LocalDate.of(2026, 1, 1));
    }

    @Test
    void kstDateOfRollsOverToNextDayAtKstMidnightBoundary() {
        // UTC 15:00:00 -> KST 다음날 00:00:00
        LocalDateTime storedUtcNaive = LocalDateTime.of(2026, 1, 1, 15, 0, 0);

        assertThat(KstClock.kstDateOf(storedUtcNaive)).isEqualTo(LocalDate.of(2026, 1, 2));
    }

    @Test
    void utcBoundOfIsInverseOfKstDateOfAtMidnight() {
        LocalDate kstDate = LocalDate.of(2026, 3, 10);

        LocalDateTime utcBound = KstClock.utcBoundOf(kstDate);

        assertThat(utcBound).isEqualTo(LocalDateTime.of(2026, 3, 9, 15, 0, 0));
        assertThat(KstClock.kstDateOf(utcBound)).isEqualTo(kstDate);
    }

    @Test
    void mondayOfWeekReturnsSameDateWhenAlreadyMonday() {
        LocalDate monday = LocalDate.of(2026, 3, 9);

        assertThat(KstClock.mondayOfWeek(monday)).isEqualTo(monday);
    }

    @Test
    void mondayOfWeekReturnsPrecedingMondayForOtherWeekdays() {
        LocalDate sunday = LocalDate.of(2026, 3, 15);

        assertThat(KstClock.mondayOfWeek(sunday)).isEqualTo(LocalDate.of(2026, 3, 9));
    }
}
