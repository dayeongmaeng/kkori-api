package com.kkori.api.admin.support;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;

/**
 * DB에 저장된 timestamp(created_at 등)는 timestamp without time zone이며 UTC wall-clock으로 기록된다:
 * kkori-api의 postgres/api 컨테이너 어디에도 TZ 설정이 없어 두 베이스 이미지(postgres:16-alpine,
 * eclipse-temurin:21-jre-alpine) 모두 기본값인 UTC로 동작한다. application.yaml의
 * spring.jackson.time-zone: Asia/Seoul은 JSON 직렬화 표기값일 뿐 저장값과 무관하다.
 * 이 가정은 운영 서버에서 재검증되지 않았으므로(AGENTS.md 참고), 실제로 다르다면 이 클래스만 고치면 된다.
 */
public final class KstClock {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private KstClock() {
    }

    public static LocalDate kstDateOf(LocalDateTime storedUtcNaive) {
        return storedUtcNaive.atZone(ZoneOffset.UTC).withZoneSameInstant(ZONE).toLocalDate();
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    /** kstDate 자정(KST)에 대응하는 UTC naive LocalDateTime 하한(포함)을 반환한다. */
    public static LocalDateTime utcBoundOf(LocalDate kstDate) {
        return kstDate.atStartOfDay(ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    public static LocalDate mondayOfWeek(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
