package com.tayota.operationservice.repository.car;

import com.tayota.operationservice.entity.car.CarArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CarArticleRepository extends JpaRepository<CarArticle, UUID> {
    // Tìm bài viết theo phiên bản xe
    List<CarArticle> findByCarVersionIdAndPublishedTrue(UUID carVersionId);
    List<CarArticle> findByCarVersionIsNullAndPublishedTrue(Sort sort);
    List<CarArticle> findAll(Sort sort);
}
