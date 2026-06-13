package com.learn.shopapi.config;

import com.learn.shopapi.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Vi du JOB DINH KY (@Scheduled): moi ngay luc 00:00 ghi log tong doanh thu.
 * Du an that thuong dung de tong hop bao cao, gui email, don dep du lieu...
 * (Can @EnableScheduling - da bat o ShopApiApplication.)
 */
@Component
public class ReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReportScheduler.class);

    private final ReportService reportService;

    public ReportScheduler(ReportService reportService) {
        this.reportService = reportService;
    }

    // cron: giay phut gio ngay thang thu. "0 0 0 * * *" = 00:00:00 moi ngay.
    @Scheduled(cron = "0 0 0 * * *")
    public void logDailyRevenue() {
        BigDecimal total = reportService.totalRevenue(null, null);
        log.info("[BAO CAO NGAY] Tong doanh thu hien tai: {}", total);
    }
}
