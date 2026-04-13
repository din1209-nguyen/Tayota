package com.nguyendin.authservice.entity;

import com.nguyendin.authservice.entity.enums.ProviderType;
import com.nguyendin.authservice.entity.enums.RoleType;
import com.nguyendin.authservice.entity.enums.StatusType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "\"USER\"")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Email
    @Size(max = 120)
    @Column(name = "email", nullable = false, unique = true, length = 120)
    private String email;

    @Size(max = 255)
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'LOCAL'")
    @Column(name = "login_provider", columnDefinition = "provider_type", nullable = false)
    private ProviderType loginProvider = ProviderType.LOCAL;

    @Size(max = 120)
    @Column(name = "provider_user_id", unique = true, length = 120)
    private String providerUserId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'USER'")
    @Column(name = "role", columnDefinition = "role_type", nullable = false)
    private RoleType role = RoleType.USER;

    @NotNull
    @CreationTimestamp
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'UNVERIFIED'")
    @Column(name = "status", columnDefinition = "status_type", nullable = false)
    private StatusType status = StatusType.UNVERIFIED;
}