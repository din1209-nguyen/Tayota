package com.nguyendin.carservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CarVersionListResponse implements Serializable {
    private List<CarVersionItemResponse> data;
    private PaginationResponse pagination;
}
