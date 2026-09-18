package com.kkori.api.admin.support;

import java.time.LocalDate;
import java.util.Set;

/** 리텐션 코호트 구성원. signupWeekStart는 가입일(KST)이 속한 월요일 시작 주, recordDates는 본인 소유 펫들의 기록 date 합집합. */
public record CohortMember(Long userId, LocalDate signupWeekStart, Set<LocalDate> recordDates) {
}
