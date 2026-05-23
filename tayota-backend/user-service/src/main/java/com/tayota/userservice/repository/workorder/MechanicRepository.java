package com.tayota.userservice.repository.workorder;

import com.tayota.userservice.entity.workorder.Mechanic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// Repository để quản lý thông tin thợ sửa xe (Mechanic) trong hệ thống
public interface MechanicRepository extends JpaRepository<Mechanic, UUID> {
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

    interface MechanicView {
        UUID getId();

        String getFullName();

        String getSpecialty();

        BigDecimal getAverageRating();
    }
}
