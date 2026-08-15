package com.bakeflow.identity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class IdentityDtos {
    private IdentityDtos() {}

    public record LoginInput(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 128) String password) {}

    public record TokenResponse(String accessToken, UserView user) {}

    public record UserView(UUID id, String name, String email, boolean active, Instant lastLoginAt,
            Set<String> roles, Set<String> permissions) {}

    public record UserInput(
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 128) String password,
            @NotEmpty Set<@NotBlank String> roles,
            boolean active) {}

    public record UserUpdate(
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Email @Size(max = 254) String email,
            @NotEmpty Set<@NotBlank String> roles,
            boolean active) {}

    public record PasswordChange(
            @NotBlank @Size(max = 128) String currentPassword,
            @NotBlank @Size(min = 8, max = 128) String newPassword) {}

    public record SessionView(UUID id, Instant createdAt, Instant lastUsedAt, Instant expiresAt,
            String userAgent, boolean current) {}
    public record UserPage(List<UserView> content, long total, int page, int size) {}
}
