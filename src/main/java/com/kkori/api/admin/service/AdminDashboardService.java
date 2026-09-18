package com.kkori.api.admin.service;

import com.kkori.api.admin.dto.request.AdminDashboardPeriodRequest;
import com.kkori.api.admin.dto.request.DashboardUnit;
import com.kkori.api.admin.dto.response.dashboard.AdminDashboardConversionResponse;
import com.kkori.api.admin.dto.response.dashboard.AdminDashboardMembersResponse;
import com.kkori.api.admin.dto.response.dashboard.AdminDashboardOverviewResponse;
import com.kkori.api.admin.dto.response.dashboard.AdminDashboardPetsResponse;
import com.kkori.api.admin.dto.response.dashboard.AdminDashboardRecordsResponse;
import com.kkori.api.admin.dto.response.dashboard.DurationStats;
import com.kkori.api.admin.dto.response.dashboard.TimeSeriesPoint;
import com.kkori.api.admin.support.ActivityEvent;
import com.kkori.api.admin.support.CohortMember;
import com.kkori.api.admin.support.DateEntry;
import com.kkori.api.admin.support.KstClock;
import com.kkori.api.admin.support.PetEligibility;
import com.kkori.api.admin.support.RecordsMetricsCalculator;
import com.kkori.api.admin.support.RetentionCalculator;
import com.kkori.api.log.entity.DailyLog;
import com.kkori.api.log.repository.DailyLogRepository;
import com.kkori.api.pet.entity.Pet;
import com.kkori.api.pet.repository.PetRepository;
import com.kkori.api.photo.entity.DailyPhoto;
import com.kkori.api.photo.repository.DailyPhotoRepository;
import com.kkori.api.user.entity.User;
import com.kkori.api.user.entity.UserStatus;
import com.kkori.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 꼬리 앱 관리자 대시보드 집계. Admin DB에는 아무것도 저장하지 않고 매 요청마다 kkori-api 자체 DB를
 * 실시간 조회해 집계한다(꾸튜디오 admin-api가 이 서비스를 다시 admin_db 없이 프록시하는 것과 같은 원칙).
 * caregiver는 지표에서 제외한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final DailyLogRepository dailyLogRepository;
    private final DailyPhotoRepository dailyPhotoRepository;

    public AdminDashboardOverviewResponse getOverview(AdminDashboardPeriodRequest period) {
        LocalDate today = KstClock.today();
        long totalMembers = userRepository.countByStatus(UserStatus.ACTIVE);

        List<ActivityEvent> todayEvents = activityEventsBetween(today, today);
        long todayRecordCount = countRawEntries(today, today);
        long dau = RecordsMetricsCalculator.distinctUnitCount(todayEvents);
        long wau = RecordsMetricsCalculator.distinctUnitCount(activityEventsBetween(today.minusDays(6), today));
        long mau = RecordsMetricsCalculator.distinctUnitCount(activityEventsBetween(today.minusDays(29), today));
        double stickiness = mau == 0 ? 0 : (double) dau / mau;

        List<ActivityEvent> periodEvents = activityEventsBetween(period.from(), period.to());
        List<TimeSeriesPoint> activeUnitTrend =
                RecordsMetricsCalculator.activeUnitTrend(periodEvents, period.from(), period.to(), period.unit());

        return new AdminDashboardOverviewResponse(totalMembers, dau, wau, mau, stickiness, todayRecordCount, activeUnitTrend);
    }

    public AdminDashboardMembersResponse getMembers(AdminDashboardPeriodRequest period) {
        long totalMembers = userRepository.countByStatus(UserStatus.ACTIVE);
        long withdrawnMembers = userRepository.countByStatus(UserStatus.WITHDRAWN);

        List<User> signedUp = userRepository.findByCreatedAtBetween(
                KstClock.utcBoundOf(period.from()), KstClock.utcBoundOf(period.to().plusDays(1)));
        List<User> withdrawn = userRepository.findByDeletedAtBetween(
                KstClock.utcBoundOf(period.from()), KstClock.utcBoundOf(period.to().plusDays(1)));

        List<TimeSeriesPoint> signupTrend = RecordsMetricsCalculator.countTrendByKstDate(
                signedUp.stream().map(u -> KstClock.kstDateOf(u.getCreatedAt())).toList(), period.from(), period.to(), period.unit());
        List<TimeSeriesPoint> withdrawalTrend = RecordsMetricsCalculator.countTrendByKstDate(
                withdrawn.stream().map(u -> KstClock.kstDateOf(u.getDeletedAt())).toList(), period.from(), period.to(), period.unit());

        Map<String, Long> byProvider = new LinkedHashMap<>();
        for (var provider : com.kkori.api.user.entity.OAuthProvider.values()) {
            byProvider.put(provider.name(), 0L);
        }
        for (User user : userRepository.findByDeletedAtIsNull()) {
            if (user.getProvider() != null) {
                byProvider.merge(user.getProvider().name(), 1L, Long::sum);
            }
        }

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (UserStatus status : UserStatus.values()) {
            byStatus.put(status.name(), userRepository.countByStatus(status));
        }

        return new AdminDashboardMembersResponse(totalMembers, withdrawnMembers, signupTrend, withdrawalTrend, byProvider, byStatus);
    }

    public AdminDashboardPetsResponse getPets(AdminDashboardPeriodRequest period) {
        List<Pet> petsAsOfTo = petRepository.findByCreatedAtLessThan(KstClock.utcBoundOf(period.to().plusDays(1)));
        List<Pet> existingAsOfTo = petsAsOfTo.stream().filter(p -> existsAsOf(p, period.to())).toList();

        Map<Long, User> activeMembers = userRepository.findByStatus(UserStatus.ACTIVE).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        long totalPets = existingAsOfTo.size();

        List<LocalDate> newPetDates = petsAsOfTo.stream()
                .map(p -> KstClock.kstDateOf(p.getCreatedAt()))
                .filter(d -> !d.isBefore(period.from()) && !d.isAfter(period.to()))
                .toList();
        List<TimeSeriesPoint> newPetTrend =
                RecordsMetricsCalculator.countTrendByKstDate(newPetDates, period.from(), period.to(), period.unit());

        Map<String, Long> bySpecies = new LinkedHashMap<>();
        for (var species : com.kkori.api.pet.entity.Species.values()) {
            bySpecies.put(species.name(), 0L);
        }
        existingAsOfTo.forEach(p -> bySpecies.merge(p.getSpecies().name(), 1L, Long::sum));

        Map<String, Long> petCountPerActiveMember = new HashMap<>();
        for (Pet pet : existingAsOfTo) {
            if (pet.getUserId() != null && activeMembers.containsKey(pet.getUserId())) {
                petCountPerActiveMember.merge("m" + pet.getUserId(), 1L, Long::sum);
            }
        }
        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put("0", 0L);
        distribution.put("1", 0L);
        distribution.put("2", 0L);
        distribution.put("3", 0L);
        long membersWithPets = 0;
        for (long count : petCountPerActiveMember.values()) {
            String bucket = count >= 3 ? "3" : String.valueOf(count);
            distribution.merge(bucket, 1L, Long::sum);
            membersWithPets++;
        }
        distribution.merge("0", activeMembers.size() - membersWithPets, Long::sum);

        return new AdminDashboardPetsResponse(totalPets, newPetTrend, bySpecies, distribution);
    }

    public AdminDashboardRecordsResponse getRecords(AdminDashboardPeriodRequest period) {
        LocalDateTime rangeStart = KstClock.utcBoundOf(period.from());
        LocalDateTime rangeEndExclusive = KstClock.utcBoundOf(period.to().plusDays(1));
        List<DailyLog> logs = dailyLogRepository.findByCreatedAtBetween(rangeStart, rangeEndExclusive);
        List<DailyPhoto> photos = dailyPhotoRepository.findByCreatedAtBetween(rangeStart, rangeEndExclusive);

        Set<Long> petIds = new HashSet<>();
        logs.forEach(l -> petIds.add(l.getPetId()));
        photos.forEach(p -> petIds.add(p.getPetId()));
        Map<Long, Pet> petMap = petRepository.findAllById(petIds).stream().collect(Collectors.toMap(Pet::getId, p -> p));

        List<ActivityEvent> events = new ArrayList<>();
        for (DailyLog log : logs) {
            toOwnerKey(petMap.get(log.getPetId())).ifPresent(key ->
                    events.add(new ActivityEvent(key, KstClock.kstDateOf(log.getCreatedAt()))));
        }
        for (DailyPhoto photo : photos) {
            toOwnerKey(petMap.get(photo.getPetId())).ifPresent(key ->
                    events.add(new ActivityEvent(key, KstClock.kstDateOf(photo.getCreatedAt()))));
        }

        List<TimeSeriesPoint> photoTrend = RecordsMetricsCalculator.countTrendByKstDate(
                photos.stream().map(p -> KstClock.kstDateOf(p.getCreatedAt())).toList(), period.from(), period.to(), period.unit());
        List<TimeSeriesPoint> logTrend = RecordsMetricsCalculator.countTrendByKstDate(
                logs.stream().map(l -> KstClock.kstDateOf(l.getCreatedAt())).toList(), period.from(), period.to(), period.unit());
        List<TimeSeriesPoint> activeUnitTrend =
                RecordsMetricsCalculator.activeUnitTrend(events, period.from(), period.to(), period.unit());

        List<PetEligibility> eligibilities = eligibilitiesAsOf(period.to());
        var engagementTrend = RecordsMetricsCalculator.engagementTrend(events, eligibilities, period.from(), period.to(), period.unit());

        // 누적/streak/기능조합: date 컬럼 기준(별도 TZ 변환 불필요), 조회 기간(from~to) 내 데이터만 사용.
        List<DailyLog> logsByDate = dailyLogRepository.findByDateBetween(period.from(), period.to());
        List<DailyPhoto> photosByDate = dailyPhotoRepository.findByDateBetween(period.from(), period.to());

        List<LocalDate> allDates = new ArrayList<>();
        logsByDate.forEach(l -> allDates.add(l.getDate()));
        photosByDate.forEach(p -> allDates.add(p.getDate()));
        List<TimeSeriesPoint> cumulativeTrend =
                RecordsMetricsCalculator.cumulativeTrend(allDates, period.from(), period.to(), period.unit());

        Map<Long, Set<LocalDate>> datesByPet = new HashMap<>();
        logsByDate.forEach(l -> datesByPet.computeIfAbsent(l.getPetId(), k -> new HashSet<>()).add(l.getDate()));
        photosByDate.forEach(p -> datesByPet.computeIfAbsent(p.getPetId(), k -> new HashSet<>()).add(p.getDate()));
        var streak = RecordsMetricsCalculator.streakSummary(datesByPet, period.to());

        List<DateEntry> dateEntries = new ArrayList<>();
        logsByDate.forEach(l -> dateEntries.add(new DateEntry(l.getPetId(), l.getDate(), false)));
        photosByDate.forEach(p -> dateEntries.add(new DateEntry(p.getPetId(), p.getDate(), true)));
        Map<String, Long> featureCombination = RecordsMetricsCalculator.featureCombination(dateEntries);

        return new AdminDashboardRecordsResponse(
                photoTrend, logTrend, activeUnitTrend, engagementTrend, cumulativeTrend, streak, featureCombination);
    }

    public AdminDashboardConversionResponse getConversion(AdminDashboardPeriodRequest period) {
        List<User> signedUp = userRepository.findByCreatedAtBetween(
                KstClock.utcBoundOf(period.from()), KstClock.utcBoundOf(period.to().plusDays(1)));
        Map<Long, User> signedUpById = signedUp.stream().collect(Collectors.toMap(User::getId, u -> u));
        List<Pet> theirPets = signedUp.isEmpty() ? List.of() : petRepository.findByUserIdIn(signedUpById.keySet());
        Map<Long, List<Pet>> petsByUser = theirPets.stream().collect(Collectors.groupingBy(Pet::getUserId));

        List<Duration> signupToFirstPetDurations = new ArrayList<>();
        for (User user : signedUp) {
            List<Pet> pets = petsByUser.get(user.getId());
            if (pets == null || pets.isEmpty()) {
                continue;
            }
            Pet firstPet = pets.stream().min(Comparator.comparing(Pet::getCreatedAt)).orElseThrow();
            signupToFirstPetDurations.add(Duration.between(user.getCreatedAt(), firstPet.getCreatedAt()));
        }
        DurationStats signupToFirstPet = RetentionCalculator.durationStats(signupToFirstPetDurations);

        List<Pet> newPets = petRepository.findByCreatedAtBetween(
                KstClock.utcBoundOf(period.from()), KstClock.utcBoundOf(period.to().plusDays(1)));
        List<Long> newPetIds = newPets.stream().map(Pet::getId).toList();
        List<DailyLog> newPetLogs = newPetIds.isEmpty() ? List.of() : dailyLogRepository.findByPetIdIn(newPetIds);
        List<DailyPhoto> newPetPhotos = newPetIds.isEmpty() ? List.of() : dailyPhotoRepository.findByPetIdIn(newPetIds);
        Map<Long, LocalDateTime> firstRecordByPet = new HashMap<>();
        newPetLogs.forEach(l -> firstRecordByPet.merge(l.getPetId(), l.getCreatedAt(), (a, b) -> a.isBefore(b) ? a : b));
        newPetPhotos.forEach(p -> firstRecordByPet.merge(p.getPetId(), p.getCreatedAt(), (a, b) -> a.isBefore(b) ? a : b));

        List<Duration> petToFirstRecordDurations = new ArrayList<>();
        for (Pet pet : newPets) {
            LocalDateTime firstRecord = firstRecordByPet.get(pet.getId());
            if (firstRecord != null) {
                petToFirstRecordDurations.add(Duration.between(pet.getCreatedAt(), firstRecord));
            }
        }
        DurationStats petToFirstRecord = RetentionCalculator.durationStats(petToFirstRecordDurations);

        List<CohortMember> cohortMembers = new ArrayList<>();
        for (User user : signedUp) {
            LocalDate weekStart = KstClock.mondayOfWeek(KstClock.kstDateOf(user.getCreatedAt()));
            List<Pet> pets = petsByUser.getOrDefault(user.getId(), List.of());
            List<Long> petIds = pets.stream().map(Pet::getId).toList();
            Set<LocalDate> recordDates = new HashSet<>();
            if (!petIds.isEmpty()) {
                LocalDate windowEnd = period.to();
                dailyLogRepository.findByPetIdInAndDateBetween(petIds, weekStart, windowEnd).forEach(l -> recordDates.add(l.getDate()));
                dailyPhotoRepository.findByPetIdInAndDateBetween(petIds, weekStart, windowEnd).forEach(p -> recordDates.add(p.getDate()));
            }
            cohortMembers.add(new CohortMember(user.getId(), weekStart, recordDates));
        }
        var retention = RetentionCalculator.cohortRetention(cohortMembers, period.to());

        return new AdminDashboardConversionResponse(signupToFirstPet, petToFirstRecord, retention);
    }

    private List<ActivityEvent> activityEventsBetween(LocalDate fromKst, LocalDate toKst) {
        LocalDateTime start = KstClock.utcBoundOf(fromKst);
        LocalDateTime endExclusive = KstClock.utcBoundOf(toKst.plusDays(1));
        List<DailyLog> logs = dailyLogRepository.findByCreatedAtBetween(start, endExclusive);
        List<DailyPhoto> photos = dailyPhotoRepository.findByCreatedAtBetween(start, endExclusive);

        Set<Long> petIds = new HashSet<>();
        logs.forEach(l -> petIds.add(l.getPetId()));
        photos.forEach(p -> petIds.add(p.getPetId()));
        Map<Long, Pet> petMap = petIds.isEmpty()
                ? Map.of()
                : petRepository.findAllById(petIds).stream().collect(Collectors.toMap(Pet::getId, p -> p));

        List<ActivityEvent> events = new ArrayList<>();
        for (DailyLog log : logs) {
            toOwnerKey(petMap.get(log.getPetId())).ifPresent(key ->
                    events.add(new ActivityEvent(key, KstClock.kstDateOf(log.getCreatedAt()))));
        }
        for (DailyPhoto photo : photos) {
            toOwnerKey(petMap.get(photo.getPetId())).ifPresent(key ->
                    events.add(new ActivityEvent(key, KstClock.kstDateOf(photo.getCreatedAt()))));
        }
        return events;
    }

    private long countRawEntries(LocalDate fromKst, LocalDate toKst) {
        LocalDateTime start = KstClock.utcBoundOf(fromKst);
        LocalDateTime endExclusive = KstClock.utcBoundOf(toKst.plusDays(1));
        return dailyLogRepository.findByCreatedAtBetween(start, endExclusive).size()
                + dailyPhotoRepository.findByCreatedAtBetween(start, endExclusive).size();
    }

    private List<PetEligibility> eligibilitiesAsOf(LocalDate to) {
        List<Pet> pets = petRepository.findByCreatedAtLessThan(KstClock.utcBoundOf(to.plusDays(1)));
        Set<Long> activeUserIds = userRepository.findByStatus(UserStatus.ACTIVE).stream().map(User::getId).collect(Collectors.toSet());
        List<PetEligibility> result = new ArrayList<>();
        for (Pet pet : pets) {
            boolean ownerActive = pet.getUserId() == null || activeUserIds.contains(pet.getUserId());
            LocalDate deletedDate = pet.getDeletedAt() == null ? null : KstClock.kstDateOf(pet.getDeletedAt());
            result.add(new PetEligibility(
                    pet.getId(), ownerKey(pet), KstClock.kstDateOf(pet.getCreatedAt()), deletedDate, ownerActive));
        }
        return result;
    }

    private boolean existsAsOf(Pet pet, LocalDate day) {
        if (pet.getDeletedAt() == null) {
            return true;
        }
        return KstClock.kstDateOf(pet.getDeletedAt()).isAfter(day);
    }

    private java.util.Optional<String> toOwnerKey(Pet pet) {
        return pet == null ? java.util.Optional.empty() : java.util.Optional.of(ownerKey(pet));
    }

    private String ownerKey(Pet pet) {
        return pet.getUserId() != null ? "user:" + pet.getUserId() : "device:" + pet.getDeviceId();
    }
}
