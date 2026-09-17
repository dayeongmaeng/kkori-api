package com.kkori.api.admin.service;

import com.kkori.api.admin.dto.response.AdminMemberDetailResponse;
import com.kkori.api.admin.dto.response.AdminMemberPageResponse;
import com.kkori.api.admin.dto.response.AdminMemberSummaryResponse;
import com.kkori.api.admin.exception.AdminMemberNotFoundException;
import com.kkori.api.user.entity.User;
import com.kkori.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberService {

    private final UserRepository userRepository;

    public AdminMemberPageResponse getMembers(String keyword, int page, int size) {
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? "" : keyword.trim();
        var users = userRepository.searchActive(
                normalizedKeyword,
                PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "id"))
        );
        return AdminMemberPageResponse.from(users.map(AdminMemberSummaryResponse::from));
    }

    public AdminMemberDetailResponse getMember(String memberId) {
        return AdminMemberDetailResponse.from(requireActiveMember(memberId));
    }

    @Transactional
    public void suspendMember(String memberId, String reason) {
        User user = requireActiveMember(memberId);
        user.suspend(reason);
        log.info("Admin suspended member: memberId={}", memberId);
    }

    @Transactional
    public void forceLogoutMember(String memberId) {
        User user = requireActiveMember(memberId);
        user.forceLogout();
        log.info("Admin forced logout for member: memberId={}", memberId);
    }

    private User requireActiveMember(String memberId) {
        return userRepository.findByExternalIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new AdminMemberNotFoundException(memberId));
    }
}
