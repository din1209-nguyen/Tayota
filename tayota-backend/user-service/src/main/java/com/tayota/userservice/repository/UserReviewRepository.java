package com.tayota.userservice.repository;

import com.tayota.userservice.entity.UserReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserReviewRepository extends JpaRepository<UserReview, UUID> {
    boolean existsByServiceTicketId(UUID serviceTicketId);

    Optional<UserReview> findByServiceTicketId(UUID serviceTicketId);

    List<UserReview> findByMechanicIdOrderByCreatedAtDesc(UUID mechanicId);

    List<UserReview> findByUserIdOrderByCreatedAtDesc(UUID userId);
}