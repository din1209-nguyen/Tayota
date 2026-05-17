package com.tayota.carservice.controller;

import com.tayota.carservice.dto.Request.CreateCarVersionRequestDTO;
import com.tayota.carservice.dto.Response.CarVersionDetailResponseDTO;
import com.tayota.carservice.dto.Response.CarVersionListResponseDTO;
import com.tayota.carservice.dto.Response.CreateCarVersionResponseDTO;
import com.tayota.carservice.service.CarVersionService;
import com.tayota.commoncore.dto.ApiResponse;
import com.tayota.commoncore.exception.CustomException;
import com.tayota.commoncore.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/cars")
@RequiredArgsConstructor
public class CarVersionController {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private final CarVersionService carVersionService;

    // Lấy danh sách phiên bản xe
    @GetMapping("/versions")
    public ApiResponse<CarVersionListResponseDTO> getCarVersions(
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) String page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) String size
    ) {
        CarVersionListResponseDTO response = carVersionService.getCarVersions(
                parsePage(page),
                parseSize(size)
        );
        return ApiResponse.success(200, "Lấy danh sách phiên bản xe thành công!", response);
    }

    // Tạo phiên bản xe mới
    @PostMapping("/versions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<CreateCarVersionResponseDTO> createCarVersion(@Valid @RequestBody CreateCarVersionRequestDTO request) {
        CreateCarVersionResponseDTO response = carVersionService.createCarVersion(request);
        return ApiResponse.success(201, "Thêm phiên bản xe thành công!", response);
    }

    // Lấy thông tin chi tiết phiên bản xe
    @GetMapping("/versions/{id}")
    public ApiResponse<CarVersionDetailResponseDTO> getCarVersionDetail(@PathVariable UUID id) {
        CarVersionDetailResponseDTO response = carVersionService.getCarVersionDetail(id, getCurrentUserId());
        return ApiResponse.success(200, "Lấy chi tiết phiên bản xe thành công!", response);
    }

    private UUID getCurrentUserId() {
        try {
            String currentUserId = SecurityUtil.getCurrentUserId();
            return currentUserId == null ? null : UUID.fromString(currentUserId);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return null;
        }
    }

    private int parsePage(String value) {
        int page = parseInteger(value, "Trang không hợp lệ");
        if (page < 0) {
            throw new CustomException(400, "Trang không hợp lệ");
        }
        return page;
    }

    private int parseSize(String value) {
        int size = parseInteger(value, "Kích thước trang không hợp lệ");
        if (size <= 0 || size > MAX_SIZE) {
            throw new CustomException(400, "Kích thước trang không hợp lệ");
        }
        return size;
    }

    private int parseInteger(String value, String message) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new CustomException(400, message);
        }
    }
}
