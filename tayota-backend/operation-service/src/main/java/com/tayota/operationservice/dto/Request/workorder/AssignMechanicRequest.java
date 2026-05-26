package com.tayota.operationservice.dto.request.workorder;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.UUID;

@Getter
public class AssignMechanicRequest {
    @NotNull(message = "Vui lòng chọn kỹ thuật viên")
    private UUID mechanicId;
}
