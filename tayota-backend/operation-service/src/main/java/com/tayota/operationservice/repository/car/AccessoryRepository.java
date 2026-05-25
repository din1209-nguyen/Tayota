package com.tayota.operationservice.repository.car;

import com.tayota.operationservice.entity.car.Accessory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AccessoryRepository extends JpaRepository<Accessory, UUID>, JpaSpecificationExecutor<Accessory> {
    // Lọc danh sách phụ kiện theo dòng xe hoặc phiên bản xe
    @Query("""
            select distinct a from Accessory a
            left join CarAccessory ca on ca.accessory = a
            left join ca.carVersion cv
            left join cv.carSeries cs
            where (lower(a.model) like :keywordPattern
                or lower(a.brand) like :keywordPattern
                or lower(a.type) like :keywordPattern)
            and lower(a.type) like :typePattern
            and (:versionId is null or cv.id = :versionId)
            and (:seriesId is null or cs.id = :seriesId)
            """)
    Page<Accessory> search(
            @Param("keywordPattern") String keywordPattern,
            @Param("typePattern") String typePattern,
            @Param("seriesId") UUID seriesId,
            @Param("versionId") UUID versionId,
            Pageable pageable
    );
}
