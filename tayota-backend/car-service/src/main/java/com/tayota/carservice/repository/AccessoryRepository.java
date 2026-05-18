package com.tayota.carservice.repository;

import com.tayota.carservice.entity.Accessory;
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
            where (:keyword is null
                or lower(a.model) like lower(concat('%', :keyword, '%'))
                or lower(a.brand) like lower(concat('%', :keyword, '%'))
                or lower(a.type) like lower(concat('%', :keyword, '%')))
            and (:type is null or lower(a.type) = lower(:type))
            and (:versionId is null or cv.id = :versionId)
            and (:seriesId is null or cs.id = :seriesId)
            """)
    Page<Accessory> search(
            @Param("keyword") String keyword,
            @Param("type") String type,
            @Param("seriesId") UUID seriesId,
            @Param("versionId") UUID versionId,
            Pageable pageable
    );
}
