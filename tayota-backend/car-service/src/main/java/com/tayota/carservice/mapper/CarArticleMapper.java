package com.tayota.carservice.mapper;

import com.tayota.carservice.dto.Response.CarArticleResponseDTO;
import com.tayota.carservice.entity.CarArticle;
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
