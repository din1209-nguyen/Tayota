package com.tayota.carservice.repository;

import com.tayota.carservice.entity.CarVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface CarVersionRepository extends JpaRepository<CarVersion, UUID>, JpaSpecificationExecutor<CarVersion> {
    // Tìm phiên bản xe theo dòng xe
    List<CarVersion> findByCarSeriesId(UUID carSeriesId);

    // Kiểm tra tên phiên bản đã tồn tại trong cùng dòng xe
    boolean existsByNameAndCarSeriesId(String name, UUID carSeriesId);

    // Kiểm tra tên phiên bản đã tồn tại trong cùng dòng xe ngoài bản ghi hiện tại
    boolean existsByNameAndCarSeriesIdAndIdNot(String name, UUID carSeriesId, UUID id);

    // Lọc danh sách phiên bản xe theo điều kiện tìm kiếm
    @Query("""
            select distinct cv from CarVersion cv
            join cv.carSeries cs
            join cs.carStyle st
            left join CarPrice cp on cp.carVersion = cv
            where (:keyword is null
                or lower(cv.name) like lower(concat('%', :keyword, '%'))
                or lower(cs.name) like lower(concat('%', :keyword, '%'))
                or lower(st.name) like lower(concat('%', :keyword, '%')))
            and (:styleId is null or st.id = :styleId)
            and (:seriesId is null or cs.id = :seriesId)
            and (:modelYear is null or cv.modelYear = :modelYear)
            and (:minPrice is null or cp.price >= :minPrice)
            and (:maxPrice is null or cp.price <= :maxPrice)
            """)
    Page<CarVersion> search(
            @Param("keyword") String keyword,
            @Param("styleId") UUID styleId,
            @Param("seriesId") UUID seriesId,
            @Param("modelYear") Integer modelYear,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );
}
