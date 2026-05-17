package com.tayota.carservice.dto.Response;

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
public class CarVersionListResponseDTO implements Serializable {
    private List<CarVersionItemResponseDTO> data;
    private PaginationResponseDTO pagination;
}
