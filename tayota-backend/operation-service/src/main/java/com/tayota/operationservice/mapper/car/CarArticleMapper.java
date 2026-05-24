package com.tayota.operationservice.mapper.car;

import com.tayota.operationservice.dto.response.car.CarArticleResponseDTO;
import com.tayota.operationservice.entity.car.CarArticle;
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
