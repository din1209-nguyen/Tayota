package com.tayota.operationservice.repository.user;

import com.tayota.operationservice.entity.user.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    // Dùng để lấy thông tin liên hệ của user đã đăng nhập cho màn quản lý lịch hẹn
    @Query("""
            select userProfile.fullname as fullname,
                   userProfile.user.email as email,
                   userProfile.phone as phone
            from UserProfile userProfile
            where userProfile.id = :userId
            """)
    Optional<UserContactView> findContactByUserId(@Param("userId") UUID userId);

    // Dùng để lưu thông tin liên hê của user
    interface UserContactView {
        String getFullname();

        String getEmail();

        String getPhone();
    }

}
