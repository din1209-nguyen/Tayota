package com.tayota.operationservice.repository.user;

import com.tayota.operationservice.entity.user.User;
import com.tayota.operationservice.enums.user.ProviderType;
import com.tayota.operationservice.enums.user.StatusType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    // Tìm kiếm người dùng theo email
    Optional<User> findByEmail(String email);

    // Tìm kiếm người dùng theo loginProvider và providerUserId (dành cho đăng nhập Google)
    Optional<User> findByLoginProviderAndProviderUserId(ProviderType loginProvider, String providerUserId);

    // Kiểm tra xem email đã tồn tại
    boolean existsByEmail(String email);

    // Cập nhật mật khẩu người dùng theo ID
    @Modifying
    @Query("UPDATE User u SET u.passwordHash = :passwordHash WHERE u.id = :id")
    void updatePasswordHashById(UUID id, String passwordHash);

    // Cập nhật trạng thái người dùng theo ID
    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.id = :id")
    void updateStatusById(UUID id, StatusType status);
}
