package com.tayota.userservice.entity;

import com.tayota.userservice.enums.ProviderType;
import com.tayota.commoncore.enums.RoleType;
import com.tayota.userservice.enums.StatusType;
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
    private UUID id;

    @NotNull
    @Email
    @Size(max = 120)
    private String email;

    @Size(max = 255)
    private String passwordHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'LOCAL'")
    private ProviderType loginProvider = ProviderType.LOCAL;

    @Size(max = 120)
    private String providerUserId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'USER'")
    private RoleType role = RoleType.USER;


    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'ACTIVE'")
    private StatusType status = StatusType.ACTIVE;

    @CreationTimestamp
    @ColumnDefault("CURRENT_TIMESTAMP")
    private Instant createdAt;
}
