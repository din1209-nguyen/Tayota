package com.tayota.operationservice.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class ManagerUserStatsResponse {
    private long total;
    private Map<String, Long> byRole;
    private Map<String, Long> byStatus;
}
