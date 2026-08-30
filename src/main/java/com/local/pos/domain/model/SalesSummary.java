package com.local.pos.domain.model;

import java.math.BigDecimal;

public record SalesSummary(int totalSales,
                           BigDecimal cashTotal,
                           BigDecimal cardTotal,
                           BigDecimal otherTotal,
                           BigDecimal grandTotal) {
}
