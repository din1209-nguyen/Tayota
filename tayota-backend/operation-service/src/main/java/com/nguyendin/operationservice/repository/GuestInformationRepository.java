package com.nguyendin.operationservice.repository;

import com.nguyendin.operationservice.entity.GuestInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GuestInformationRepository extends JpaRepository<GuestInformation, UUID> {
}