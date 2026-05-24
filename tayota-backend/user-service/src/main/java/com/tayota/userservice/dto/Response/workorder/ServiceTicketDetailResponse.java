package com.tayota.userservice.dto.Response.workorder;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor

public class ServiceTicketDetailResponse {
    private ServiceTicketInfoResponse serviceTicket;
    private List<ServiceItemResponse> items;
}
