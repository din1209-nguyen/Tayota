package com.tayota.userservice.repository;

import com.tayota.userservice.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

}
