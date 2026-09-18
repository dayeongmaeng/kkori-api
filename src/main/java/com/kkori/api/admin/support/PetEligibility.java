package com.kkori.api.admin.support;

import java.time.LocalDate;

/**
 * 참여율 분모 계산용. createdDate/deletedDate는 KST 날짜, ownerActive는 비회원이거나
 * 소유 회원이 "현재" ACTIVE·미삭제인지(과거 시점 상태는 추적하지 않으므로 현재값으로 근사).
 */
public record PetEligibility(Long petId, String ownerKey, LocalDate createdDate, LocalDate deletedDate, boolean ownerActive) {

    public boolean existsOn(LocalDate day) {
        return !createdDate.isAfter(day) && (deletedDate == null || deletedDate.isAfter(day));
    }

    public boolean eligibleOn(LocalDate day) {
        return existsOn(day) && ownerActive;
    }
}
