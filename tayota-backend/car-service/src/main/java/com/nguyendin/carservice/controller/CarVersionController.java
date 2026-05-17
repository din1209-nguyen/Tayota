package com.nguyendin.carservice.controller;

import com.nguyendin.carservice.dto.CarVersionListResponse;
import com.nguyendin.carservice.dto.CreateCarVersionRequest;
import com.nguyendin.carservice.dto.ErrorResponse;
import com.nguyendin.carservice.service.CarVersionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private static final String ADMIN_ROLE = "ROLE_ADMIN";
    private static final String MANAGER_ROLE = "ROLE_MANAGER";

    private final CarVersionService carVersionService;

    @GetMapping("/versions")
    public ResponseEntity<?> getCarVersions(
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) String page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) String size
    ) {
        Integer parsedPage = parseInteger(page);
        Integer parsedSize = parseInteger(size);

        if (parsedPage == null || parsedSize == null || parsedPage < 0 || parsedSize <= 0 || parsedSize > MAX_SIZE) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(400, "Sai tham số"));
        }

        CarVersionListResponse response = carVersionService.getCarVersions(parsedPage, parsedSize);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/versions")
    public ResponseEntity<?> createCarVersion(
            @Valid @RequestBody CreateCarVersionRequest request,
            BindingResult bindingResult
    ) {
        if (!hasManagerPermission()) {
            return forbidden();
        }

        if (bindingResult.hasErrors()) {
            return badRequest();
        }

        try {
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(carVersionService.createCarVersion(request));
        } catch (IllegalArgumentException exception) {
            return badRequest();
        }
    }

    @GetMapping("/versions/{id}")
    public ResponseEntity<?> getCarVersionDetail(@PathVariable String id) {
        UUID carVersionId = parseUuid(id);

        if (carVersionId == null) {
            return notFound();
        }

        return carVersionService.getCarVersionDetail(carVersionId, getCurrentUserId())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(this::notFound);
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }

        return parseUuid(authentication.getPrincipal().toString());
    }

    private boolean hasManagerPermission() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> ADMIN_ROLE.equals(role) || MANAGER_ROLE.equals(role));
    }

    private ResponseEntity<ErrorResponse> badRequest() {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Sai dữ liệu"));
    }

    private ResponseEntity<ErrorResponse> forbidden() {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(403, "Không có quyền"));
    }

    private ResponseEntity<ErrorResponse> notFound() {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, "Không tìm thấy"));
    }
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentNotValidException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidRequestBody() {
        return badRequest();
    }
}
