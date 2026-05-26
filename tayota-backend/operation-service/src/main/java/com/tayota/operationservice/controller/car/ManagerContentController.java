package com.tayota.operationservice.controller.car;

import com.tayota.operationservice.dto.common.ApiResponse;
import com.tayota.operationservice.dto.request.car.CarArticleRequestDTO;
import com.tayota.operationservice.dto.request.car.DealershipRequestDTO;
import com.tayota.operationservice.dto.response.car.AccessoryResponseDTO;
import com.tayota.operationservice.dto.response.car.CarArticleResponseDTO;
import com.tayota.operationservice.dto.response.car.CarVersionDetailResponseDTO;
import com.tayota.operationservice.dto.response.car.CarVersionItemResponseDTO;
import com.tayota.operationservice.dto.response.car.DealershipResponseDTO;
import com.tayota.operationservice.dto.response.car.PaginationResponseDTO;
import com.tayota.operationservice.service.car.AccessoryService;
import com.tayota.operationservice.service.car.ArticleService;
import com.tayota.operationservice.service.car.CarCatalogService;
import com.tayota.operationservice.service.car.CarVersionService;
import com.tayota.operationservice.service.car.DealershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/manager")
@PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
public class ManagerContentController {
    private final CarVersionService carVersionService;
    private final CarCatalogService carCatalogService;
    private final AccessoryService accessoryService;
    private final ArticleService articleService;
    private final DealershipService dealershipService;

    @GetMapping("/car-versions")
    public ApiResponse<List<CarVersionItemResponseDTO>> getCarVersions() {
        return ApiResponse.success(200, "Lấy danh sách phiên bản quản trị thành công.", carVersionService.getCarVersionsForManagement());
    }

    @GetMapping("/car-versions/{carVersionId}")
    public ApiResponse<CarVersionDetailResponseDTO> getCarVersionDetail(@PathVariable String carVersionId) {
        return ApiResponse.success(200, "Lấy chi tiết phiên bản quản trị thành công.", carCatalogService.getCarVersionDetailForManagement(carVersionId));
    }

    @GetMapping("/accessories")
    public ApiResponse<PaginationResponseDTO<AccessoryResponseDTO>> getAccessories(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String seriesId,
            @RequestParam(required = false) String versionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.success(200, "Lấy danh sách phụ kiện quản trị thành công.",
                accessoryService.searchAccessoriesForManagement(keyword, type, seriesId, versionId, page, size));
    }

    @GetMapping("/articles")
    public ApiResponse<List<CarArticleResponseDTO>> getArticles() {
        return ApiResponse.success(200, "Lấy danh sách bài viết quản trị thành công.", articleService.getArticlesForManagement());
    }

    @PostMapping("/articles")
    public ApiResponse<CarArticleResponseDTO> createArticle(@Valid @RequestBody CarArticleRequestDTO request) {
        return ApiResponse.success(201, "Thêm bài viết thành công.", articleService.createArticle(request));
    }

    @PutMapping("/articles/{articleId}")
    public ApiResponse<CarArticleResponseDTO> updateArticle(
            @PathVariable String articleId, @Valid @RequestBody CarArticleRequestDTO request) {
        return ApiResponse.success(200, "Cập nhật bài viết thành công.", articleService.updateArticle(articleId, request));
    }

    @DeleteMapping("/articles/{articleId}")
    public ApiResponse<Void> hideArticle(@PathVariable String articleId) {
        articleService.hideArticle(articleId);
        return ApiResponse.success(200, "Đã ẩn bài viết.", null);
    }

    @GetMapping("/dealerships")
    public ApiResponse<List<DealershipResponseDTO>> getDealerships() {
        return ApiResponse.success(200, "Lấy danh sách đại lý quản trị thành công.", dealershipService.getDealershipsForManagement());
    }

    @PostMapping("/dealerships")
    public ApiResponse<DealershipResponseDTO> createDealership(@Valid @RequestBody DealershipRequestDTO request) {
        return ApiResponse.success(201, "Thêm đại lý thành công.", dealershipService.createDealership(request));
    }

    @PutMapping("/dealerships/{dealershipId}")
    public ApiResponse<DealershipResponseDTO> updateDealership(
            @PathVariable String dealershipId, @Valid @RequestBody DealershipRequestDTO request) {
        return ApiResponse.success(200, "Cập nhật đại lý thành công.", dealershipService.updateDealership(dealershipId, request));
    }

    @DeleteMapping("/dealerships/{dealershipId}")
    public ApiResponse<Void> deactivateDealership(@PathVariable String dealershipId) {
        dealershipService.deactivateDealership(dealershipId);
        return ApiResponse.success(200, "Đã ngừng hoạt động đại lý.", null);
    }
}
