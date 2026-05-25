package com.tayota.operationservice.controller.car;

import com.tayota.operationservice.dto.common.ApiResponse;
import com.tayota.operationservice.dto.response.car.CarArticleResponseDTO;
import com.tayota.operationservice.service.car.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/news")
public class NewsController {
    private final ArticleService articleService;

    @GetMapping
    public ApiResponse<List<CarArticleResponseDTO>> getNews() {
        return ApiResponse.success(200, "Lấy danh sách tin tức thành công.", articleService.getPublishedNews());
    }

    @GetMapping("/{articleId}")
    public ApiResponse<CarArticleResponseDTO> getArticle(@PathVariable String articleId) {
        return ApiResponse.success(200, "Lấy bài viết thành công.", articleService.getPublishedArticle(articleId));
    }
}
