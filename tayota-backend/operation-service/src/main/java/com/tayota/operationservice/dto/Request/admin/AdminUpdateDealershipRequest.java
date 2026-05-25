package com.tayota.operationservice.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUpdateDealershipRequest {
    @NotBlank(message = "Đại lý không được để trống")
    private String dealershipId;
}
