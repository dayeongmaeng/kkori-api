package com.kkori.api.admin.support;

import com.kkori.api.admin.dto.request.DashboardUnit;
import com.kkori.api.admin.dto.response.dashboard.EngagementPoint;
import com.kkori.api.admin.dto.response.dashboard.StreakSummary;
import com.kkori.api.admin.dto.response.dashboard.TimeSeriesPoint;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RecordsMetricsCalculatorTest {

    private static final LocalDate FROM = LocalDate.of(2026, 3, 1);
    private static final LocalDate TO = LocalDate.of(2026, 3, 7);

    @Test
    void activeUnitTrendCountsEachUnitOnceEvenWithMultipleEventsSameDay() {
        List<ActivityEvent> events = List.of(
                new ActivityEvent("user:1", LocalDate.of(2026, 3, 1)),
                new ActivityEvent("user:1", LocalDate.of(2026, 3, 1)), // 같은 회원의 두 번째 기록(사진+일지 등) -> 중복 집계 안 됨
                new ActivityEvent("device:9", LocalDate.of(2026, 3, 1)) // 비회원 펫은 device 단위로 별도 집계
        );

        List<TimeSeriesPoint> trend = RecordsMetricsCalculator.activeUnitTrend(events, FROM, TO, DashboardUnit.DAY);

        TimeSeriesPoint firstDay = trend.get(0);
        assertThat(firstDay.bucketStart()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(firstDay.value()).isEqualTo(2);
    }

    @Test
    void engagementTrendExcludesPetDeletedBeforeBucketDayFromEligibleDenominator() {
        // 3/1에 생성, 3/3에 삭제된 펫: 3/1~3/2는 분모 포함, 3/3부터는 제외.
        PetEligibility deletedMidPeriod = new PetEligibility(
                1L, "user:1", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 3), true);

        List<EngagementPoint> trend = RecordsMetricsCalculator.engagementTrend(
                List.of(), List.of(deletedMidPeriod), FROM, TO, DashboardUnit.DAY);

        EngagementPoint mar2 = trend.stream().filter(p -> p.bucketStart().equals(LocalDate.of(2026, 3, 2))).findFirst().orElseThrow();
        EngagementPoint mar3 = trend.stream().filter(p -> p.bucketStart().equals(LocalDate.of(2026, 3, 3))).findFirst().orElseThrow();
        assertThat(mar2.eligibleUnits()).isEqualTo(1);
        assertThat(mar3.eligibleUnits()).isEqualTo(0);
    }

    @Test
    void engagementTrendExcludesPetOwnedByWithdrawnMember() {
        PetEligibility ownedByWithdrawnMember = new PetEligibility(
                1L, "user:1", LocalDate.of(2026, 3, 1), null, false); // ownerActive=false (탈퇴 등으로 현재 비활성)
        PetEligibility ownedByNonMember = new PetEligibility(
                2L, "device:9", LocalDate.of(2026, 3, 1), null, true); // 비회원 펫은 항상 ownerActive=true

        List<EngagementPoint> trend = RecordsMetricsCalculator.engagementTrend(
                List.of(), List.of(ownedByWithdrawnMember, ownedByNonMember), FROM, TO, DashboardUnit.DAY);

        EngagementPoint mar1 = trend.get(0);
        assertThat(mar1.eligibleUnits()).isEqualTo(1);
    }

    @Test
    void cumulativeTrendAccumulatesAcrossBuckets() {
        List<LocalDate> dates = List.of(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 3));

        List<TimeSeriesPoint> trend = RecordsMetricsCalculator.cumulativeTrend(dates, FROM, TO, DashboardUnit.DAY);

        assertThat(pointOn(trend, LocalDate.of(2026, 3, 1)).value()).isEqualTo(2);
        assertThat(pointOn(trend, LocalDate.of(2026, 3, 2)).value()).isEqualTo(2);
        assertThat(pointOn(trend, LocalDate.of(2026, 3, 3)).value()).isEqualTo(3);
        assertThat(pointOn(trend, LocalDate.of(2026, 3, 7)).value()).isEqualTo(3);
    }

    @Test
    void streakSummaryCountsConsecutiveDaysEndingAtTo() {
        Set<LocalDate> continuousUpToTo = new HashSet<>(Set.of(
                LocalDate.of(2026, 3, 5), LocalDate.of(2026, 3, 6), LocalDate.of(2026, 3, 7)));
        Set<LocalDate> brokenBeforeTo = new HashSet<>(Set.of(LocalDate.of(2026, 3, 1)));

        StreakSummary summary = RecordsMetricsCalculator.streakSummary(
                Map.of(1L, continuousUpToTo, 2L, brokenBeforeTo), TO);

        assertThat(summary.maxCurrentStreakDays()).isEqualTo(3);
        assertThat(summary.distribution().get("1-3")).isEqualTo(1L); // pet 1: 3일 연속
        assertThat(summary.distribution().get("0")).isEqualTo(1L);   // pet 2: to(3/7)에 기록 없음 -> streak 0
    }

    @Test
    void featureCombinationClassifiesPhotoOnlyLogOnlyAndBothPerPetDate() {
        List<DateEntry> entries = List.of(
                new DateEntry(1L, LocalDate.of(2026, 3, 1), true),  // pet1: photo only
                new DateEntry(2L, LocalDate.of(2026, 3, 1), false), // pet2: log only
                new DateEntry(3L, LocalDate.of(2026, 3, 1), true),
                new DateEntry(3L, LocalDate.of(2026, 3, 1), false)  // pet3: both (같은 날 사진+일지)
        );

        Map<String, Long> result = RecordsMetricsCalculator.featureCombination(entries);

        assertThat(result).containsEntry("PHOTO_ONLY", 1L).containsEntry("LOG_ONLY", 1L).containsEntry("BOTH", 1L);
    }

    private TimeSeriesPoint pointOn(List<TimeSeriesPoint> trend, LocalDate date) {
        return trend.stream().filter(p -> p.bucketStart().equals(date)).findFirst().orElseThrow();
    }
}
