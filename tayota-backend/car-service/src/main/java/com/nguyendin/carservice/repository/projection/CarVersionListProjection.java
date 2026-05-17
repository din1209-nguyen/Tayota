package com.nguyendin.carservice.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface CarVersionListProjection {
    UUID getId();

    String getVersion();

    String getSeries();

    String getStyle();

    BigDecimal getMinPrice();

    BigDecimal getSalePercent();

    String getImageUrl();
}
