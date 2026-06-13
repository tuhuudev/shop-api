package com.learn.shopapi.controller;

import com.learn.shopapi.dto.CategoryRevenueView;
import com.learn.shopapi.dto.CustomerSpendingView;
import com.learn.shopapi.dto.DailyRevenueView;
import com.learn.shopapi.dto.MonthlyRevenueView;
import com.learn.shopapi.dto.ProductSalesView;
import com.learn.shopapi.service.ReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Cac endpoint bao cao - phan "Data". Chi STAFF/ADMIN duoc xem.
 * Loc theo khoang ngay: ?from=YYYY-MM-DD&to=YYYY-MM-DD (deu optional, mac dinh toan bo lich su).
 */
@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('STAFF','ADMIN')")
@Tag(name = "Reports", description = "Bao cao doanh thu (STAFF/ADMIN)")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/best-sellers")
    public List<ProductSalesView> bestSellers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "50") int limit) {
        return reportService.bestSellers(from, to, limit);
    }

    @GetMapping("/daily-revenue")
    public List<DailyRevenueView> dailyRevenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.dailyRevenue(from, to);
    }

    @GetMapping("/total-revenue")
    public Map<String, BigDecimal> totalRevenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return Map.of("totalRevenue", reportService.totalRevenue(from, to));
    }

    @GetMapping("/revenue-by-category")
    public List<CategoryRevenueView> revenueByCategory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.revenueByCategory(from, to);
    }

    @GetMapping("/revenue-by-month")
    public List<MonthlyRevenueView> revenueByMonth(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.revenueByMonth(from, to);
    }

    @GetMapping("/top-customers")
    public List<CustomerSpendingView> topCustomers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "50") int limit) {
        return reportService.topCustomers(from, to, limit);
    }

    /** Xuat bao cao ban chay ra CSV de tai ve (Content-Disposition: attachment). */
    @GetMapping(value = "/best-sellers/csv", produces = "text/csv")
    public ResponseEntity<String> bestSellersCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        StringBuilder sb = new StringBuilder("productName,totalQuantity,totalRevenue\n");
        for (ProductSalesView v : reportService.bestSellers(from, to, 1000)) {
            sb.append(escape(v.getProductName())).append(',')
              .append(v.getTotalQuantity()).append(',')
              .append(v.getTotalRevenue()).append('\n');
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=best-sellers.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(sb.toString());
    }

    /** Boc gia tri co dau phay/nhay kep theo chuan CSV. */
    private String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
