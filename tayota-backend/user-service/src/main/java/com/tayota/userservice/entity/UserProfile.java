package com.tayota.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "\"USER_PROFILE\"")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfile {

    @Id
    @Column(updatable = false)
    UUID id;

    @Column(nullable = false, length = 40)
    String fullname;

    @Column(length = 10)
    String phone;

    Boolean gender;

    LocalDate birthDate;

    String address;

    String avatarUrl;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = false)
    @MapsId
    User user;
}
