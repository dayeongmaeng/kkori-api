package com.kkori.api.admin.support;

import java.time.LocalDate;

/** daily_log/daily_photo의 date 컬럼 기준 원본 엔트리(누적·streak·기능조합 계산용). */
public record DateEntry(Long petId, LocalDate date, boolean isPhoto) {
}
