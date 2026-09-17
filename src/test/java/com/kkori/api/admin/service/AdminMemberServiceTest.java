package com.kkori.api.admin.service;

import com.kkori.api.admin.dto.response.AdminMemberDetailResponse;
import com.kkori.api.admin.dto.response.AdminMemberPageResponse;
import com.kkori.api.admin.exception.AdminMemberNotFoundException;
import com.kkori.api.user.entity.OAuthProvider;
import com.kkori.api.user.entity.User;
import com.kkori.api.user.entity.UserStatus;
import com.kkori.api.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMemberServiceTest {

    @Mock
    private UserRepository userRepository;

    private AdminMemberService adminMemberService;

    @BeforeEach
    void setUp() {
        adminMemberService = new AdminMemberService(userRepository);
    }

    @Test
    void getMembersMapsPageOfUsersToSummaries() {
        User user = user(1L, "member-1", "nick", "user@example.com");
        when(userRepository.searchActive(eq("nick"), any())).thenReturn(new PageImpl<>(List.of(user)));

        AdminMemberPageResponse response = adminMemberService.getMembers("nick", 0, 20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).memberId()).isEqualTo("member-1");
        assertThat(response.content().get(0).nickname()).isEqualTo("nick");
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void getMembersTreatsBlankKeywordAsNoFilter() {
        when(userRepository.searchActive(eq(""), any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        adminMemberService.getMembers("   ", 0, 20);

        verify(userRepository).searchActive(eq(""), any(PageRequest.class));
    }

    @Test
    void getMemberReturnsDetailForExistingMember() {
        User user = user(1L, "member-1", "nick", "user@example.com");
        when(userRepository.findByExternalIdAndDeletedAtIsNull("member-1")).thenReturn(Optional.of(user));

        AdminMemberDetailResponse response = adminMemberService.getMember("member-1");

        assertThat(response.memberId()).isEqualTo("member-1");
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void getMemberThrowsWhenMissing() {
        when(userRepository.findByExternalIdAndDeletedAtIsNull(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminMemberService.getMember("missing"))
                .isInstanceOf(AdminMemberNotFoundException.class);
    }

    @Test
    void suspendMemberUpdatesStatusReasonAndInvalidatesSession() {
        User user = user(1L, "member-1", "nick", "user@example.com");
        when(userRepository.findByExternalIdAndDeletedAtIsNull("member-1")).thenReturn(Optional.of(user));

        adminMemberService.suspendMember("member-1", "약관 위반");

        assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(user.isSuspended()).isTrue();
        assertThat(user.getSuspensionReason()).isEqualTo("약관 위반");
        assertThat(user.isSessionInvalidatedAfter(java.time.Instant.now().minusSeconds(60))).isTrue();
    }

    @Test
    void forceLogoutMemberInvalidatesSessionWithoutChangingStatus() {
        User user = user(1L, "member-1", "nick", "user@example.com");
        when(userRepository.findByExternalIdAndDeletedAtIsNull("member-1")).thenReturn(Optional.of(user));

        adminMemberService.forceLogoutMember("member-1");

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.isSessionInvalidatedAfter(java.time.Instant.now().minusSeconds(60))).isTrue();
    }

    private User user(Long id, String externalId, String nickname, String email) {
        User user = User.builder()
                .externalId(externalId)
                .provider(OAuthProvider.GOOGLE)
                .providerUserId("google-" + externalId)
                .email(email)
                .nickname(nickname)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
