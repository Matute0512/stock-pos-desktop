package com.local.pos.domain.model;

import java.math.BigDecimal;

public record LowStockProduct(long productId,
                              String productName,
                              BigDecimal stockQty,
                              BigDecimal minStockQty) {
}
