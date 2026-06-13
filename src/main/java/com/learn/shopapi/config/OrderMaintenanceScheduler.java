package com.learn.shopapi.config;

import com.learn.shopapi.entity.OrderStatus;
import com.learn.shopapi.repository.OrderRepository;
import com.learn.shopapi.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JOB: tu dong HUY don PENDING qua han -> nha lai ton kho da giu luc tao don.
 * Tranh tinh trang don khong thanh toan giu kho vinh vien.
 *
 * Multi-instance: job chay tren MOI replica, nhung viec huy + hoan kho moi don di qua
 * OrderService.autoCancelStale (tx rieng + @Version tren Order) nen chi 1 instance huy thanh cong
 * moi don, khong hoan kho trung -> KHONG can khoa phan tan.
 */
@Component
public class OrderMaintenanceScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderMaintenanceScheduler.class);

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final boolean enabled;
    private final long timeoutMinutes;

    public OrderMaintenanceScheduler(
            OrderRepository orderRepository,
            OrderService orderService,
            @Value("${app.orders.auto-cancel.enabled:true}") boolean enabled,
            @Value("${app.orders.auto-cancel.timeout-minutes:30}") long timeoutMinutes) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.enabled = enabled;
        this.timeoutMinutes = timeoutMinutes;
    }

    // Quet moi 5 phut. initialDelay tranh chay ngay luc khoi dong (cho context on dinh).
    @Scheduled(fixedDelayString = "${app.orders.auto-cancel.interval-ms:300000}", initialDelay = 60000)
    public void cancelStalePendingOrders() {
        if (!enabled) {
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<Long> ids = orderRepository.findIdsByStatusAndOrderDateBefore(OrderStatus.PENDING, cutoff);
        if (ids.isEmpty()) {
            return;
        }
        int cancelled = 0;
        for (Long id : ids) {
            try {
                if (orderService.autoCancelStale(id, cutoff)) {
                    cancelled++;
                }
            } catch (RuntimeException e) {
                // optimistic-lock (instance khac vua huy) hoac loi le -> bo qua, lan quet sau xu ly tiep.
                log.debug("Bo qua don id={} khi auto-cancel: {}", id, e.toString());
            }
        }
        if (cancelled > 0) {
            log.info("[AUTO-CANCEL] Da huy {} don PENDING qua {} phut, hoan kho.", cancelled, timeoutMinutes);
        }
    }
}
