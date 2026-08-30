package com.local.pos.domain.model;

import java.math.BigDecimal;

public record StockValuation(long productId,
                             String productName,
                             BigDecimal stockQty,
                             BigDecimal cost,
                             BigDecimal price,
                             BigDecimal valueAtCost,
                             BigDecimal valueAtPrice) {
}
