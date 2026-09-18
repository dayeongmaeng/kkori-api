package com.kkori.api.admin.support;

import com.kkori.api.admin.dto.response.dashboard.CohortRetention;
import com.kkori.api.admin.dto.response.dashboard.DurationStats;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RetentionCalculatorTest {

    @Test
    void durationStatsExcludesNegativeDurations() {
        List<Duration> durations = List.of(
                Duration.ofHours(10), Duration.ofHours(20), Duration.ofHours(-5) // 음수: 데이터 이상치, 제외
        );

        DurationStats stats = RetentionCalculator.durationStats(durations);

        assertThat(stats.sampleCount()).isEqualTo(2);
        assertThat(stats.avgHours()).isEqualTo(15.0);
        assertThat(stats.medianHours()).isEqualTo(15.0);
    }

    @Test
    void durationStatsReturnsEmptyWhenAllDurationsAreNegative() {
        DurationStats stats = RetentionCalculator.durationStats(List.of(Duration.ofHours(-1)));

        assertThat(stats).isEqualTo(DurationStats.EMPTY);
    }

    @Test
    void cohortRetentionComputesW1RateAndExcludesMembersWithNoActivityInWindow() {
        LocalDate cohortStart = LocalDate.of(2026, 1, 5); // 월요일
        LocalDate asOf = LocalDate.of(2026, 2, 1); // W1만 성숙, W8은 미성숙

        CohortMember retainedInWeek1 = new CohortMember(1L, cohortStart, Set.of(LocalDate.of(2026, 1, 13)));
        CohortMember notRetained = new CohortMember(2L, cohortStart, Set.of(LocalDate.of(2026, 1, 5)));

        List<CohortRetention> result = RetentionCalculator.cohortRetention(
                List.of(retainedInWeek1, notRetained), asOf);

        CohortRetention cohort = result.get(0);
        assertThat(cohort.cohortSize()).isEqualTo(2);
        assertThat(cohort.w1()).isEqualTo(0.5);
    }

    @Test
    void cohortRetentionReturnsNullForWeekNotYetElapsed() {
        LocalDate cohortStart = LocalDate.of(2026, 1, 5);
        LocalDate asOf = LocalDate.of(2026, 1, 10); // W1 윈도우(1/12~1/18)도 아직 안 지남

        List<CohortRetention> result = RetentionCalculator.cohortRetention(
                List.of(new CohortMember(1L, cohortStart, Set.of())), asOf);

        assertThat(result.get(0).w1()).isNull();
        assertThat(result.get(0).w8()).isNull();
    }
}
