package com.tayota.operationservice.service.car;

import com.tayota.operationservice.dto.request.car.CarArticleRequestDTO;
import com.tayota.operationservice.dto.response.car.CarArticleResponseDTO;
import com.tayota.operationservice.entity.car.CarArticle;
import com.tayota.operationservice.entity.car.CarVersion;
import com.tayota.operationservice.exception.CustomException;
import com.tayota.operationservice.mapper.car.CarArticleMapper;
import com.tayota.operationservice.repository.car.CarArticleRepository;
import com.tayota.operationservice.repository.car.CarVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArticleService {
    private final CarArticleRepository articleRepository;
    private final CarVersionRepository carVersionRepository;
    private final CarArticleMapper articleMapper;

    @Transactional(readOnly = true)
    public List<CarArticleResponseDTO> getPublishedNews() {
        return articleRepository.findByCarVersionIsNullAndPublishedTrue(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream().map(articleMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CarArticleResponseDTO getPublishedArticle(String articleId) {
        CarArticle article = findArticle(articleId);
        if (!article.isPublished()
                || (article.getCarVersion() != null && !article.getCarVersion().isVisible())) {
            throw new CustomException(404, "Không tìm thấy bài viết.");
        }
        return articleMapper.toResponse(article);
    }

    @Transactional(readOnly = true)
    public List<CarArticleResponseDTO> getArticlesForManagement() {
        return articleRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream().map(articleMapper::toResponse).toList();
    }

    @CacheEvict(value = "catalogVersionDetail", allEntries = true)
    @Transactional
    public CarArticleResponseDTO createArticle(CarArticleRequestDTO request) {
        CarArticle article = CarArticle.builder()
                .carVersion(resolveCarVersion(request.getCarVersionId()))
                .type(request.getType())
                .title(request.getTitle())
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .published(request.getPublished() == null || request.getPublished())
                .build();
        return articleMapper.toResponse(articleRepository.save(article));
    }

    @CacheEvict(value = "catalogVersionDetail", allEntries = true)
    @Transactional
    public CarArticleResponseDTO updateArticle(String articleId, CarArticleRequestDTO request) {
        CarArticle article = findArticle(articleId);
        article.setCarVersion(resolveCarVersion(request.getCarVersionId()));
        article.setType(request.getType());
        article.setTitle(request.getTitle());
        article.setContent(request.getContent());
        article.setImageUrl(request.getImageUrl());
        if (request.getPublished() != null) {
            article.setPublished(request.getPublished());
        }
        return articleMapper.toResponse(articleRepository.save(article));
    }

    @CacheEvict(value = "catalogVersionDetail", allEntries = true)
    @Transactional
    public void hideArticle(String articleId) {
        CarArticle article = findArticle(articleId);
        article.setPublished(false);
        articleRepository.save(article);
    }

    private CarVersion resolveCarVersion(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return carVersionRepository.findById(UUID.fromString(value))
                    .orElseThrow(() -> new CustomException(404, "Không tìm thấy phiên bản xe."));
        } catch (IllegalArgumentException exception) {
            throw new CustomException(400, "Id phiên bản xe không hợp lệ.");
        }
    }

    private CarArticle findArticle(String value) {
        try {
            return articleRepository.findById(UUID.fromString(value))
                    .orElseThrow(() -> new CustomException(404, "Không tìm thấy bài viết."));
        } catch (IllegalArgumentException exception) {
            throw new CustomException(400, "Id bài viết không hợp lệ.");
        }
    }
}
