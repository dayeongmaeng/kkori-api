package com.kkori.api.admin.support;

import com.kkori.api.admin.dto.response.dashboard.CohortRetention;
import com.kkori.api.admin.dto.response.dashboard.DurationStats;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** "전환·리텐션" 탭 순수 계산 로직. */
public final class RetentionCalculator {

    private static final int[] WEEK_OFFSETS = {1, 2, 4, 8};

    private RetentionCalculator() {
    }

    /** 음수 소요시간(데이터 이상치)은 제외한다. */
    public static DurationStats durationStats(List<Duration> durations) {
        List<Double> hours = durations.stream()
                .filter(d -> !d.isNegative())
                .map(d -> d.toSeconds() / 3600.0)
                .sorted()
                .toList();
        if (hours.isEmpty()) {
            return DurationStats.EMPTY;
        }
        double avg = hours.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        return new DurationStats(avg, median(hours), hours.size());
    }

    private static double median(List<Double> sorted) {
        int n = sorted.size();
        if (n % 2 == 1) {
            return sorted.get(n / 2);
        }
        return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }

    /** asOf: 리텐션 성숙도 판단 기준일(KST). 해당 주차 윈도우가 asOf 이후까지 걸쳐 있으면 아직 판단할 수 없으므로 null을 반환한다. */
    public static List<CohortRetention> cohortRetention(List<CohortMember> members, LocalDate asOf) {
        Map<LocalDate, List<CohortMember>> byCohort = members.stream()
                .collect(Collectors.groupingBy(CohortMember::signupWeekStart));
        List<CohortRetention> result = new ArrayList<>();
        for (Map.Entry<LocalDate, List<CohortMember>> entry : new TreeMap<>(byCohort).entrySet()) {
            LocalDate cohortStart = entry.getKey();
            List<CohortMember> cohort = entry.getValue();
            Double[] rates = new Double[WEEK_OFFSETS.length];
            for (int i = 0; i < WEEK_OFFSETS.length; i++) {
                rates[i] = retentionRate(cohort, cohortStart, WEEK_OFFSETS[i], asOf);
            }
            result.add(new CohortRetention(cohortStart, cohort.size(), rates[0], rates[1], rates[2], rates[3]));
        }
        return result;
    }

    private static Double retentionRate(List<CohortMember> cohort, LocalDate cohortStart, int weekOffset, LocalDate asOf) {
        LocalDate windowStart = cohortStart.plusWeeks(weekOffset);
        LocalDate windowEnd = windowStart.plusDays(6);
        if (windowEnd.isAfter(asOf) || cohort.isEmpty()) {
            return null;
        }
        long retained = cohort.stream()
                .filter(m -> m.recordDates().stream().anyMatch(d -> !d.isBefore(windowStart) && !d.isAfter(windowEnd)))
                .count();
        return (double) retained / cohort.size();
    }
}
