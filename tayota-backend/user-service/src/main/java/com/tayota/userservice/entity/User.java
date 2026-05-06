package com.tayota.userservice.entity;

import com.tayota.userservice.enums.ProviderType;
import com.tayota.commoncore.enums.RoleType;
import com.tayota.userservice.enums.StatusType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "\"USER\"")
public class User {

    // Đăng nhập truyền thống
    public static User createLocalUser(String email, String passwordHash) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        return user;
    }

    // Đăng nhập qua Google
    public static User createGoogleUser(String email, String providerUserId) {
        User user = new User();
        user.setEmail(email);
        user.setProviderUserId(providerUserId);
        user.setLoginProvider(ProviderType.GOOGLE);
        return user;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "login_provider", nullable = false, columnDefinition = "providertype DEFAULT 'LOCAL'")
    private ProviderType loginProvider = ProviderType.LOCAL;

    @Column(name = "provider_user_id", unique = true, length = 120)
    private String providerUserId;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "roletype DEFAULT 'USER'")
    private RoleType role = RoleType.USER;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "statustype DEFAULT 'ACTIVE'")
    private StatusType status = StatusType.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt = Instant.now();
}
