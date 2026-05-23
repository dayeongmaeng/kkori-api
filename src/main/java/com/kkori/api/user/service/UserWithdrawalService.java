package com.kkori.api.user.service;

import com.kkori.api.auth.context.AuthenticatedUser;
import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
import com.kkori.api.pet.service.PetService;
import com.kkori.api.user.entity.OAuthProvider;
import com.kkori.api.user.entity.User;
import com.kkori.api.user.event.UserWithdrawalEvent;
import com.kkori.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserWithdrawalService {

    private final UserRepository userRepository;
    private final PetService petService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 회원 탈퇴 처리.
     *
     * 하나의 트랜잭션 안에서:
     *   - 소유 반려동물 + 연관 기록/사진 cascade soft delete
     *   - 이미지 파일 비동기 삭제 이벤트 발행 (AFTER_COMMIT, 기존 PetImageCleanupListener 재사용)
     *   - 사용자 개인정보 익명화 및 WITHDRAWN 처리
     *
     * 트랜잭션 커밋 후 (AFTER_COMMIT):
     *   - OAuth 연결 해제 시도 (OAuthDisconnectListener, 비동기)
     *   - OAuth 해제 실패 시 로그만 기록하고 탈퇴 API 결과에는 영향 없음
     *
     * 확장 포인트 (추후 가족공유 도입 시):
     *   - OWNER 탈퇴: 소유권 이전 요구 또는 남은 멤버 없을 때 pet 삭제
     *   - MEMBER 탈퇴: PetMember 관계만 WITHDRAWN 처리하고 pet 데이터 유지
     *   - petService.deleteAllForUser() 호출 전에 역할에 따라 분기 처리
     *
     * TODO (법적 보관):
     *   - 결제/환불/약관동의/접속로그 등은 archive 또는 legal_hold 테이블로 이관 후 여기서 호출
     *   - 현재 해당 도메인 미존재로 생략
     */
    @Transactional
    public void withdraw(AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        if (user.isDeleted() || user.isWithdrawn()) {
            throw new BusinessException(ErrorCode.USER_002);
        }

        OAuthProvider originalProvider = user.getProvider();
        String originalProviderUserId = user.getProviderUserId();
        Long userId = user.getId();

        log.info("[Withdraw] withdrawal start: userId={}, provider={}", userId, originalProvider);

        // 1. 소유 반려동물 cascade soft delete (DailyLog, DailyLogPhoto, DailyPhoto 포함)
        //    이미지 파일 삭제는 PetImageCleanupEvent → PetImageCleanupListener(AFTER_COMMIT, @Async)
        petService.deleteAllForUser(userId);

        // 2. 개인정보 익명화 및 WITHDRAWN 상태 변경
        user.withdraw();

        // 3. OAuth 연결 해제는 AFTER_COMMIT 비동기 처리 (탈퇴 API 성패에 영향 없음)
        eventPublisher.publishEvent(new UserWithdrawalEvent(userId, originalProvider, originalProviderUserId));

        log.info("[Withdraw] user withdrawal complete: userId={}, provider={}", userId, originalProvider);
    }
}
