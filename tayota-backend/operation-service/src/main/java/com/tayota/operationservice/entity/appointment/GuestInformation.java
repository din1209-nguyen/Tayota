package com.tayota.operationservice.entity.appointment;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "\"GUEST_INFORMATION\"")
public class GuestInformation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 250)
    private String email;

    @Column(nullable = false, length = 10)
    private String phone;
}