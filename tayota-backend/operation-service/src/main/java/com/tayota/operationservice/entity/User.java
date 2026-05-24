package com.tayota.operationservice.entity;

import com.tayota.commoncore.enums.RoleType;
import com.tayota.operationservice.enums.ProviderType;
import com.tayota.operationservice.enums.StatusType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "\"USER\"")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false)
    UUID id;

    @Column(unique = true, nullable = false)
    String email;

    @Column(length = 60)
    String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    ProviderType loginProvider = ProviderType.LOCAL;

    @Column(unique = true, length = 120)
    String providerUserId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    RoleType role = RoleType.USER;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    StatusType status = StatusType.ACTIVE;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    Instant createdAt;
}
