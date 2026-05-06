package com.tayota.userservice.repository;

import com.tayota.userservice.entity.User;
import com.tayota.userservice.enums.ProviderType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    // Tìm kiếm người dùng theo email
    Optional<User> findByEmail(String email);

    // Tìm kiếm người dùng theo loginProvider và providerUserId (dành cho đăng nhập Google)
    Optional<User> findByLoginProviderAndProviderUserId(ProviderType loginProvider, String providerUserId);

    // Kiểm tra xem email đã tồn tại
    boolean existsByEmail(String email);
}
