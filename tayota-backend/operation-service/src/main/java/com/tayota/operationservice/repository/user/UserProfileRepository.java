package com.tayota.operationservice.repository.user;

import com.tayota.operationservice.entity.user.UserProfile;
import com.tayota.operationservice.enums.user.RoleType;
import com.tayota.operationservice.enums.user.StatusType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Collection;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    @Query(
            value = """
                    select userProfile
                    from UserProfile userProfile
                    join userProfile.user user
                    where (:role is null or user.role = :role)
                      and (:status is null or user.status = :status)
                    order by user.createdAt desc
                    """,
            countQuery = """
                    select count(userProfile)
                    from UserProfile userProfile
                    join userProfile.user user
                    where (:role is null or user.role = :role)
                      and (:status is null or user.status = :status)
                    """
    )
    Page<UserProfile> searchForAdminWithoutKeyword(
            @Param("role") RoleType role,
            @Param("status") StatusType status,
            Pageable pageable
    );

    @Query(
            value = """
                    select userProfile
                    from UserProfile userProfile
                    join userProfile.user user
                    where (:role is null or user.role = :role)
                      and (:status is null or user.status = :status)
                      and (
                           lower(user.email) like concat('%', :keyword, '%')
                           or lower(userProfile.fullname) like concat('%', :keyword, '%')
                           or userProfile.phone like concat('%', :keyword, '%')
                      )
                    order by user.createdAt desc
                    """,
            countQuery = """
                    select count(userProfile)
                    from UserProfile userProfile
                    join userProfile.user user
                    where (:role is null or user.role = :role)
                      and (:status is null or user.status = :status)
                      and (
                           lower(user.email) like concat('%', :keyword, '%')
                           or lower(userProfile.fullname) like concat('%', :keyword, '%')
                           or userProfile.phone like concat('%', :keyword, '%')
                      )
                    """
    )
    Page<UserProfile> searchForAdminWithKeyword(
            @Param("keyword") String keyword,
            @Param("role") RoleType role,
            @Param("status") StatusType status,
            Pageable pageable
    );

    @Query(
            value = """
                    select userProfile from UserProfile userProfile join userProfile.user user
                    where user.role in :roles
                      and (:role is null or user.role = :role)
                      and (:status is null or user.status = :status)
                    order by user.createdAt desc
                    """,
            countQuery = """
                    select count(userProfile) from UserProfile userProfile join userProfile.user user
                    where user.role in :roles
                      and (:role is null or user.role = :role)
                      and (:status is null or user.status = :status)
                    """
    )
    Page<UserProfile> searchForManagerWithoutKeyword(
            @Param("roles") Collection<RoleType> roles,
            @Param("role") RoleType role,
            @Param("status") StatusType status,
            Pageable pageable
    );

    @Query(
            value = """
                    select userProfile from UserProfile userProfile join userProfile.user user
                    where user.role in :roles
                      and (:role is null or user.role = :role)
                      and (:status is null or user.status = :status)
                      and (lower(user.email) like concat('%', :keyword, '%')
                        or lower(userProfile.fullname) like concat('%', :keyword, '%')
                        or userProfile.phone like concat('%', :keyword, '%'))
                    order by user.createdAt desc
                    """,
            countQuery = """
                    select count(userProfile) from UserProfile userProfile join userProfile.user user
                    where user.role in :roles
                      and (:role is null or user.role = :role)
                      and (:status is null or user.status = :status)
                      and (lower(user.email) like concat('%', :keyword, '%')
                        or lower(userProfile.fullname) like concat('%', :keyword, '%')
                        or userProfile.phone like concat('%', :keyword, '%'))
                    """
    )
    Page<UserProfile> searchForManagerWithKeyword(
            @Param("roles") Collection<RoleType> roles,
            @Param("keyword") String keyword,
            @Param("role") RoleType role,
            @Param("status") StatusType status,
            Pageable pageable
    );

    // Dùng để lấy thông tin liên hệ của user đã đăng nhập cho màn quản lý lịch hẹn
    @Query("""
            select userProfile.fullname as fullname,
                   userProfile.user.email as email,
                   userProfile.phone as phone
            from UserProfile userProfile
            where userProfile.id = :userId
            """)
    Optional<UserContactView> findContactByUserId(@Param("userId") UUID userId);

    @Query("""
            select userProfile
            from UserProfile userProfile
            join userProfile.user user
            where user.role = :role
              and user.status = :status
              and (
                   lower(user.email) like concat('%', :keyword, '%')
                   or lower(userProfile.fullname) like concat('%', :keyword, '%')
                   or userProfile.phone like concat('%', :keyword, '%')
              )
            order by userProfile.fullname asc
            """)
    Page<UserProfile> searchActiveCustomersForAdvisor(
            @Param("keyword") String keyword,
            @Param("role") RoleType role,
            @Param("status") StatusType status,
            Pageable pageable
    );

    // Dùng để lưu thông tin liên hê của user
    interface UserContactView {
        String getFullname();

        String getEmail();

        String getPhone();
    }

}
