package com.tayota.operationservice.car.mapper;

import com.tayota.operationservice.car.dto.Response.CarArticleResponseDTO;
import com.tayota.operationservice.car.entity.CarArticle;
import org.springframework.stereotype.Component;

@Component
public class CarArticleMapper {

    // Chuyển bài viết sang response
    public CarArticleResponseDTO toResponse(CarArticle article) {
        return new CarArticleResponseDTO(
                article.getId(),
                article.getType(),
                article.getTitle(),
                article.getContent(),
                article.getImageUrl()
        );
    }
}
