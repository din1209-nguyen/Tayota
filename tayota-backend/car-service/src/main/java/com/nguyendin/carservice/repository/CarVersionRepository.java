package com.nguyendin.carservice.repository;

import com.nguyendin.carservice.entity.CarVersion;
import com.nguyendin.carservice.repository.projection.CarVersionListProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CarVersionRepository extends JpaRepository<CarVersion, UUID> {
    List<CarVersion> findByCarSeriesId(UUID carSeriesId);
    List<CarVersion> findByVersionContainingIgnoreCase(String version);
    boolean existsByCarSeriesIdAndVersion(UUID carSeriesId, String version);

    @EntityGraph(attributePaths = {"carSeries", "carSeries.carStyle"})
    Optional<CarVersion> findWithCarSeriesById(UUID id);

    @Query(
            value = """
                    SELECT
                        cv.id AS id,
                        cv.version AS version,
                        cs.name AS series,
                        style.name AS style,
                        MIN(cp.price) AS minPrice,
                        cv.salePercent AS salePercent,
                        cv.imageUrl AS imageUrl
                    FROM CarVersion cv
                    JOIN cv.carSeries cs
                    JOIN cs.carStyle style
                    LEFT JOIN CarPrice cp ON cp.carVersion = cv
                    GROUP BY cv.id, cv.version, cs.name, style.name, cv.salePercent, cv.imageUrl
                    ORDER BY cs.name ASC, cv.version ASC
                    """,
            countQuery = "SELECT COUNT(cv) FROM CarVersion cv"
    )
    Page<CarVersionListProjection> findCarVersionList(Pageable pageable);
}

