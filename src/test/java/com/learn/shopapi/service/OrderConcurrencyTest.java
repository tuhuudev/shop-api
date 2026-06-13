package com.learn.shopapi.service;

import com.learn.shopapi.dto.OrderRequest;
import com.learn.shopapi.entity.Product;
import com.learn.shopapi.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiem chung OPTIMISTIC LOCKING: 2 don dat cung 1 san pham con DUNG 1 cai trong kho,
 * chay DONG THOI -> chi 1 don thanh cong, kho KHONG bao gio bi am.
 *
 * Day la loi "race condition" kinh dien neu thieu @Version: ca 2 doc thay stock=1,
 * ca 2 cung tru -> ban am. @Version khien commit sau that bai (OptimisticLockException).
 */
@SpringBootTest
class OrderConcurrencyTest {

    @Autowired private OrderService orderService;
    @Autowired private ProductRepository productRepository;

    @Test
    void haiDonDongThoi_chiMotThanhCong_khoKhongAm() throws Exception {
        // San pham chi con 1 trong kho (tai khoan "customer" da gan voi Customer "an" tu DataSeeder).
        Product p = productRepository.save(
                new Product("Hang hiem", "chi con 1", new BigDecimal("1000"), 1, null));
        Long productId = p.getId();

        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Runnable buyOne = () -> {
            // SecurityContext la thread-local -> moi luong tu set "customer" dang dang nhap.
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("customer", null,
                            List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
            try {
                startGate.await();   // cho ca 2 luong cung xuat phat
                orderService.createOrder(new OrderRequest(null,
                        List.of(new OrderRequest.OrderLine(productId, 1))));
                success.incrementAndGet();
            } catch (Exception e) {
                failed.incrementAndGet();   // optimistic lock HOAC het kho
            } finally {
                SecurityContextHolder.clearContext();
            }
        };

        pool.submit(buyOne);
        pool.submit(buyOne);
        startGate.countDown();          // ban phat lenh cho 2 luong chay
        pool.shutdown();
        pool.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);

        // Dung 1 thanh cong, 1 that bai; kho ve 0 (khong am).
        assertThat(success.get()).isEqualTo(1);
        assertThat(failed.get()).isEqualTo(1);
        assertThat(productRepository.findById(productId).orElseThrow().getStockQuantity()).isEqualTo(0);
    }
}
