package com.tayota.operationservice.repository.appointment;

import com.tayota.operationservice.entity.appointment.GuestInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GuestInformationRepository extends JpaRepository<GuestInformation, UUID> {}