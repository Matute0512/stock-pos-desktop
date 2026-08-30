package com.local.pos.domain.model;

import java.math.BigDecimal;

public record TopSellingProduct(long productId,
                                String productName,
                                BigDecimal quantitySold,
                                BigDecimal totalRevenue) {
}
