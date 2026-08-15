package com.bakeflow.identity;

import static com.bakeflow.identity.IdentityDtos.LoginInput;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.bakeflow.BakeFlowBackendApplication;
import com.bakeflow.integration.application.CacheService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = BakeFlowBackendApplication.class)
@ActiveProfiles("test")
class RefreshTokenReuseIntegrationTests {
    @Autowired private IdentityService identity;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private CacheService cache;

    @Test
    void reusedTokenCommitsRevocationForEveryTokenInItsFamily() {
        when(cache.incrementWithinLimit(anyString(), anyInt(), any())).thenReturn(true);
        String email = "reuse-" + UUID.randomUUID() + "@bakeflow.local";
        identity.ensureAdmin(email, "a secure test passphrase");

        var tokenA = identity.login(new LoginInput(email, "a secure test passphrase"), "test", "agent-a");
        identity.refresh(tokenA.refreshToken(), "agent-b");

        assertThatThrownBy(() -> identity.refresh(tokenA.refreshToken(), "attacker"))
                .isInstanceOf(IdentityException.class)
                .extracting("code").isEqualTo("REFRESH_TOKEN_REUSED");

        UUID family = jdbc.queryForObject(
                "SELECT family_id FROM refresh_tokens WHERE token_hash=?", UUID.class,
                IdentityService.sha(tokenA.refreshToken()));
        Integer familySize = jdbc.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE family_id=?", Integer.class, family);
        Integer activeTokens = jdbc.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE family_id=? AND revoked_at IS NULL", Integer.class, family);

        assertThat(familySize).isGreaterThanOrEqualTo(2);
        assertThat(activeTokens).isZero();
    }
}
