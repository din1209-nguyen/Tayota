package com.tayota.userservice.service.review;

import com.tayota.userservice.enums.review.ReviewStatus;
import com.tayota.userservice.repository.review.CustomerReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

// Đây là một scheduled task để tự động chuyển các đánh giá có trạng thái PENDING nhưng đã quá thời gian sang EXPIRED.
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerReviewExpirationScheduler {
    private final CustomerReviewRepository customerReviewRepository;

    @Transactional
    @Scheduled(cron = "${review.expiration.cron:0 20 0 * * *}", zone = "${appointment.booking.business-zone:Asia/Bangkok}")
    public void expirePendingReviews() {
        int expiredCount = customerReviewRepository.expirePendingReviews(
                ReviewStatus.PENDING,
                ReviewStatus.EXPIRED,
                Instant.now()
        );

        if (expiredCount > 0) {
            log.info("Expired {} pending customer reviews", expiredCount);
        }
    }
}
