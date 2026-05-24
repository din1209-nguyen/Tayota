package com.tayota.operationservice.car.repository;

import com.tayota.operationservice.car.entity.CarArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CarArticleRepository extends JpaRepository<CarArticle, UUID> {
    // Tìm bài viết theo phiên bản xe
    List<CarArticle> findByCarVersionId(UUID carVersionId);
}
