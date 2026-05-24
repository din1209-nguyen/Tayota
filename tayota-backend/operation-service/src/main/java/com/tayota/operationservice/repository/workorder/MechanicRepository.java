package com.tayota.operationservice.repository.workorder;

import com.tayota.operationservice.entity.workorder.Mechanic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// Repository để quản lý thông tin thợ sửa xe (Mechanic) trong hệ thống
public interface MechanicRepository extends JpaRepository<Mechanic, UUID> {
    // Lấy danh sách thợ sửa xe đang hoạt động của một đại lý cụ thể, sắp xếp theo tên và chuyên môn
    @Query("""
            select mechanic.id as id,
                   userProfile.fullname as fullName,
                   mechanic.specialty as specialty,
                   mechanic.averageRating as averageRating
            from Mechanic mechanic
            left join UserProfile userProfile on userProfile.id = mechanic.id
            where mechanic.dealershipId = :dealershipId
              and mechanic.active = true
            order by userProfile.fullname asc, mechanic.specialty asc
            """)
    List<MechanicView> findActiveMechanicsByDealershipId(@Param("dealershipId") UUID dealershipId);

    // Giao diện projection để lấy dữ liệu cần thiết cho thợ sửa xe mà không cần phải lấy toàn bộ entity
    interface MechanicView {
        UUID getId();

        String getFullName();

        String getSpecialty();

        BigDecimal getAverageRating();
    }
}
