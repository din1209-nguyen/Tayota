package com.tayota.userservice.entity;

import com.tayota.userservice.enums.ProviderType;
import com.tayota.commoncore.enums.RoleType;
import com.tayota.userservice.enums.StatusType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "\"USER\"")
public class User {

    public User() {}

    // Tạo tài khoản bởi Admin
    public User(String email, String passwordHash, RoleType role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    // Đăng nhập truyền thống
    public User(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }

    // Đăng nhập qua Google
    public User(String email, String providerUserId, ProviderType providerType) {
        this.email = email;
        this.providerUserId = providerUserId;
        this.loginProvider = providerType;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(length = 60)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ProviderType loginProvider = ProviderType.LOCAL;

    @Column(unique = true, length = 120)
    private String providerUserId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private RoleType role = RoleType.USER;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private StatusType status = StatusType.ACTIVE;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}