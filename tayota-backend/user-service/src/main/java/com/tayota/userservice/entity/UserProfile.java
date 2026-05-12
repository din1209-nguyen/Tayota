package com.tayota.userservice.entity;

import com.tayota.commoncore.enums.RoleType;
import com.tayota.userservice.enums.ProviderType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "\"USER_PROFILE\"")
public class UserProfile {

    public UserProfile() {}

    // Tạo tài khoản bởi Admin
    public UserProfile(String email, String passwordHash, RoleType role) {
        this.fullname = email.split("@")[0];
        this.user = new User(email, passwordHash, role);
    }

    // Đăng nhập truyền thống
    public UserProfile(String email, String passwordHash) {
        this.fullname = email.split("@")[0];
        this.user = new User(email, passwordHash);
    }

    // Đăng nhập qua Google
    public UserProfile(String fullname, String avatarUrl, String email, String providerUserId) {
        this.fullname = fullname;
        this.avatarUrl = avatarUrl;
        this.user = new User(email, providerUserId, ProviderType.GOOGLE);
    }


    @Id
    @Column(updatable = false)
    private UUID id;

    @Column(nullable = false, length = 40)
    private String fullname;

    @Column(length = 10)
    private String phone;

    private Boolean gender;

    private LocalDate birthDate;

    private String address;

    private String avatarUrl;

    @OneToOne(cascade = CascadeType.ALL)
    @MapsId
    @JoinColumn(referencedColumnName = "id")
    private User user;
}
