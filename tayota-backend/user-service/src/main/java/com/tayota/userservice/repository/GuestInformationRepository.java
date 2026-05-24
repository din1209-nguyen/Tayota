package com.tayota.userservice.repository;

import com.tayota.userservice.entity.GuestInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GuestInformationRepository extends JpaRepository<GuestInformation, UUID> {}