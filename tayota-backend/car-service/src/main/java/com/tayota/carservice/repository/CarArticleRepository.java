package com.tayota.carservice.repository;

import com.tayota.carservice.entity.CarArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CarArticleRepository extends JpaRepository<CarArticle, UUID> {
    List<CarArticle> findByCarVersionId(UUID carVersionId);
    List<CarArticle> findByType(String type);
    List<CarArticle> findByCarVersionIdAndType(UUID carVersionId, String type);
}

