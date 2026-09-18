package com.kkori.api.admin.support;

import java.time.LocalDate;

/** created_at(KST 변환) 기준 "활성" 판정용 이벤트. unitKey는 회원이면 "user:{id}", 비회원이면 "device:{id}". */
public record ActivityEvent(String unitKey, LocalDate kstDate) {
}
