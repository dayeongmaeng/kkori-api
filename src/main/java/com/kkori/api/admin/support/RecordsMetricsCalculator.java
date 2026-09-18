package com.kkori.api.admin.support;

import com.kkori.api.admin.dto.request.DashboardUnit;
import com.kkori.api.admin.dto.response.dashboard.EngagementPoint;
import com.kkori.api.admin.dto.response.dashboard.StreakSummary;
import com.kkori.api.admin.dto.response.dashboard.TimeSeriesPoint;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** "기록" 탭(활성/참여율/누적/streak/기능조합) 순수 계산 로직. DB/엔티티에 의존하지 않아 Mockito 없이 테스트 가능하다. */
public final class RecordsMetricsCalculator {

    private RecordsMetricsCalculator() {
    }

    public static List<TimeSeriesPoint> countTrendByKstDate(List<LocalDate> kstDates, LocalDate from, LocalDate to, DashboardUnit unit) {
        Map<LocalDate, Long> counts = kstDates.stream()
                .collect(Collectors.groupingBy(d -> DashboardBuckets.bucketStartOf(d, unit), Collectors.counting()));
        return DashboardBuckets.bucketStarts(from, to, unit).stream()
                .map(b -> new TimeSeriesPoint(b, counts.getOrDefault(b, 0L)))
                .toList();
    }

    public static long distinctUnitCount(List<ActivityEvent> events) {
        return events.stream().map(ActivityEvent::unitKey).distinct().count();
    }

    public static List<TimeSeriesPoint> activeUnitTrend(List<ActivityEvent> events, LocalDate from, LocalDate to, DashboardUnit unit) {
        Map<LocalDate, Set<String>> byBucket = groupUnitsByBucket(events, unit);
        return DashboardBuckets.bucketStarts(from, to, unit).stream()
                .map(b -> new TimeSeriesPoint(b, byBucket.getOrDefault(b, Set.of()).size()))
                .toList();
    }

    public static List<EngagementPoint> engagementTrend(List<ActivityEvent> events, List<PetEligibility> eligibilities,
            LocalDate from, LocalDate to, DashboardUnit unit) {
        Map<LocalDate, Set<String>> activeByBucket = groupUnitsByBucket(events, unit);
        List<EngagementPoint> points = new ArrayList<>();
        for (LocalDate bucket : DashboardBuckets.bucketStarts(from, to, unit)) {
            LocalDate representativeDay = DashboardBuckets.bucketEnd(bucket, unit, to);
            long eligibleCount = eligibilities.stream().filter(p -> p.eligibleOn(representativeDay)).count();
            long activeCount = activeByBucket.getOrDefault(bucket, Set.of()).size();
            double rate = eligibleCount == 0 ? 0 : (double) activeCount / eligibleCount;
            points.add(new EngagementPoint(bucket, activeCount, eligibleCount, rate));
        }
        return points;
    }

    public static List<TimeSeriesPoint> cumulativeTrend(List<LocalDate> dates, LocalDate from, LocalDate to, DashboardUnit unit) {
        Map<LocalDate, Long> perDay = dates.stream().collect(Collectors.groupingBy(d -> d, Collectors.counting()));
        List<TimeSeriesPoint> result = new ArrayList<>();
        long running = 0;
        for (LocalDate bucket : DashboardBuckets.bucketStarts(from, to, unit)) {
            LocalDate end = DashboardBuckets.bucketEnd(bucket, unit, to);
            long sumInBucket = perDay.entrySet().stream()
                    .filter(e -> !e.getKey().isBefore(bucket) && !e.getKey().isAfter(end))
                    .mapToLong(Map.Entry::getValue)
                    .sum();
            running += sumInBucket;
            result.add(new TimeSeriesPoint(bucket, running));
        }
        return result;
    }

    /** datesByPet: 조회 기간 내 기록이 1건 이상 있는 펫만 포함(그 외 펫은 streak 0으로 자명하므로 제외). */
    public static StreakSummary streakSummary(Map<Long, Set<LocalDate>> datesByPet, LocalDate to) {
        List<Integer> streaks = new ArrayList<>();
        for (Set<LocalDate> dates : datesByPet.values()) {
            if (dates.isEmpty()) {
                continue;
            }
            LocalDate cursor = to;
            int streak = 0;
            while (dates.contains(cursor)) {
                streak++;
                cursor = cursor.minusDays(1);
            }
            streaks.add(streak);
        }
        double avg = streaks.isEmpty() ? 0 : streaks.stream().mapToInt(Integer::intValue).average().orElse(0);
        int max = streaks.stream().mapToInt(Integer::intValue).max().orElse(0);
        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put("0", 0L);
        distribution.put("1-3", 0L);
        distribution.put("4-7", 0L);
        distribution.put("8-14", 0L);
        distribution.put("15+", 0L);
        for (int s : streaks) {
            String bucket = s == 0 ? "0" : s <= 3 ? "1-3" : s <= 7 ? "4-7" : s <= 14 ? "8-14" : "15+";
            distribution.merge(bucket, 1L, Long::sum);
        }
        return new StreakSummary(avg, max, distribution);
    }

    public static Map<String, Long> featureCombination(List<DateEntry> entries) {
        Map<Long, Map<LocalDate, boolean[]>> perPetDay = new HashMap<>();
        for (DateEntry entry : entries) {
            boolean[] flags = perPetDay
                    .computeIfAbsent(entry.petId(), k -> new HashMap<>())
                    .computeIfAbsent(entry.date(), k -> new boolean[2]);
            if (entry.isPhoto()) {
                flags[0] = true;
            } else {
                flags[1] = true;
            }
        }
        long photoOnly = 0;
        long logOnly = 0;
        long both = 0;
        for (Map<LocalDate, boolean[]> byDate : perPetDay.values()) {
            for (boolean[] flags : byDate.values()) {
                boolean hasPhoto = flags[0];
                boolean hasLog = flags[1];
                if (hasPhoto && hasLog) {
                    both++;
                } else if (hasPhoto) {
                    photoOnly++;
                } else {
                    logOnly++;
                }
            }
        }
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("PHOTO_ONLY", photoOnly);
        result.put("LOG_ONLY", logOnly);
        result.put("BOTH", both);
        return result;
    }

    private static Map<LocalDate, Set<String>> groupUnitsByBucket(List<ActivityEvent> events, DashboardUnit unit) {
        Map<LocalDate, Set<String>> byBucket = new HashMap<>();
        for (ActivityEvent event : events) {
            LocalDate bucket = DashboardBuckets.bucketStartOf(event.kstDate(), unit);
            byBucket.computeIfAbsent(bucket, k -> new HashSet<>()).add(event.unitKey());
        }
        return byBucket;
    }
}
