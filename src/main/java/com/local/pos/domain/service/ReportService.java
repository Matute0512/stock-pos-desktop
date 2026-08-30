package com.local.pos.domain.service;

import com.local.pos.domain.model.LowStockProduct;
import com.local.pos.domain.model.SalesSummary;
import com.local.pos.domain.model.StockValuation;
import com.local.pos.domain.model.TopSellingProduct;
import com.local.pos.persistence.dao.ReportDao;

import java.time.LocalDate;
import java.util.List;

public class ReportService {

    private final ReportDao reportDao;

    public ReportService(ReportDao reportDao) {
        this.reportDao = reportDao;
    }

    public SalesSummary getSalesSummary(LocalDate date) {
        return reportDao.getSalesSummary(date);
    }

    public List<TopSellingProduct> getTopSellingProducts(LocalDate fromDate, LocalDate toDate, int limit) {
        return reportDao.getTopSellingProducts(fromDate, toDate, limit);
    }

    public List<StockValuation> getStockValuation() {
        return reportDao.getStockValuation();
    }

    public List<LowStockProduct> getLowStockProducts() {
        return reportDao.getLowStockProducts();
    }
}
