package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.UserShare;
import org.esupportail.esupsignature.repository.UserShareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserShareServiceTest {

    private UserShareService userShareService;
    private UserShareRepository userShareRepository;

    @BeforeEach
    void setUp() {
        userShareRepository = mock(UserShareRepository.class);
        userShareService = new UserShareService(mock(GlobalProperties.class));
        ReflectionTestUtils.setField(userShareService, "userShareRepository", userShareRepository);
    }

    @Test
    void signatureWithoutDelegationUsesCurrentUser() {
        assertThat(userShareService.resolveSignatureUserEppn("principal@univ.fr", "delegate@univ.fr", null))
                .isEqualTo("principal@univ.fr");
    }

    @Test
    void delegatedSignatureWithoutOwnSignUsesPrincipal() {
        when(userShareRepository.findById(1L)).thenReturn(Optional.of(userShare(false)));

        assertThat(userShareService.resolveSignatureUserEppn("principal@univ.fr", "delegate@univ.fr", 1L))
                .isEqualTo("principal@univ.fr");
    }

    @Test
    void delegatedSignatureWithOwnSignUsesDelegate() {
        when(userShareRepository.findById(1L)).thenReturn(Optional.of(userShare(true)));

        assertThat(userShareService.resolveSignatureUserEppn("principal@univ.fr", "delegate@univ.fr", 1L))
                .isEqualTo("delegate@univ.fr");
    }

    private UserShare userShare(boolean signWithOwnSign) {
        User principal = new User();
        principal.setEppn("principal@univ.fr");
        UserShare userShare = new UserShare();
        userShare.setUser(principal);
        userShare.setSignWithOwnSign(signWithOwnSign);
        return userShare;
    }
}
