package com.learn.shopapi;

import com.jayway.jsonpath.JsonPath;
import com.learn.shopapi.entity.Product;
import com.learn.shopapi.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Integration test HTTP (Testcontainers Postgres+Redis) cho luong DA-INSTANCE qua tang controller:
 *   (1) tranh chap ton kho khi 2 don dat dong thoi,
 *   (2) optimistic-lock tren Order -> 409 khi 2 pay dong thoi,
 *   (3) Idempotency-Key tra lai ket qua cu, khong tao don trung.
 *
 * Auth dat theo TUNG REQUEST bang post-processor user(...) (KHONG @WithMockUser): SecurityContext
 * la thread-local, khong lan sang worker thread cua ExecutorService. Tai khoan "customer" da gan
 * voi Customer "an" qua Flyway seed -> createOrder resolve duoc customer.
 *
 * SKIP sach o local khong Docker (AbstractIntegrationTest @Testcontainers(disabledWithoutDocker)).
 */
@SpringBootTest
class OrderConcurrencyHttpIntegrationTest extends AbstractIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ProductRepository productRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    /**
     * (1) TRANH CHAP TON KHO: product stock=1, 2 POST /api/orders dong thoi -> dung 1x201 + 1x400.
     * La 400 (KHONG phai 409): luong tao don tru kho bang UPDATE co dieu kien atomic
     * (ProductRepository.decrementStock), luong thua nhan IllegalArgumentException "khong du ton kho"
     * -> 400, chu khong di duong optimistic-lock. Kho cuoi = 0, khong bao gio am.
     */
    @Test
    void haiDonDongThoi_motTaoThanhCong_motHetKho_khoKhongAm() throws Exception {
        Product p = productRepository.save(
                new Product("Hang hiem HTTP", "chi con 1", new BigDecimal("1000"), 1, null));
        Long productId = p.getId();
        String body = "{\"items\":[{\"productId\":" + productId + ",\"quantity\":1}]}";

        List<Integer> statuses = runConcurrently(2, () ->
                mockMvc.perform(post("/api/orders")
                                .with(user("customer").roles("CUSTOMER"))
                                .contentType(MediaType.APPLICATION_JSON).content(body))
                        .andReturn().getResponse().getStatus());

        assertThat(statuses).filteredOn(s -> s == 201).hasSize(1);
        assertThat(statuses).filteredOn(s -> s == 400).hasSize(1);
        assertThat(productRepository.findById(productId).orElseThrow().getStockQuantity()).isEqualTo(0);
    }

    /**
     * (2) OPTIMISTIC-LOCK 409: tao 1 don PENDING, 2 POST /api/orders/{id}/pay dong thoi tren cung order
     * -> dung 1x200; request con lai 409 (Order.@Version: tx commit sau nem ObjectOptimisticLockingFailureException)
     * hoac 400 (neu no doc don sau khi tx dau da commit). Thoi diem quyet dinh nen test chap nhan ca hai.
     */
    @Test
    void haiPayDongThoi_motThanhCong_motOptimisticLock409() throws Exception {
        Product p = productRepository.save(
                new Product("Hang pay HTTP", "du kho", new BigDecimal("1000"), 5, null));
        String body = "{\"items\":[{\"productId\":" + p.getId() + ",\"quantity\":1}]}";

        String created = mockMvc.perform(post("/api/orders")
                        .with(user("customer").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        Integer orderId = JsonPath.read(created, "$.id");

        List<MockHttpServletResponse> responses = runConcurrently(2, () ->
                mockMvc.perform(post("/api/orders/{id}/pay", orderId)
                                .with(user("customer").roles("CUSTOMER")))
                        .andReturn().getResponse());

        List<Integer> statuses = responses.stream().map(MockHttpServletResponse::getStatus).toList();
        // Dung 1 lan thanh toan thanh cong. Request thua co 2 ket qua hop le tuy thoi diem:
        //  - 409: hai tx doc cung version -> tx commit sau dinh optimistic lock;
        //  - 400: tx sau doc don SAU khi tx dau da commit -> don da PAID, khong chuyen trang thai duoc.
        // Ca hai deu la problem+json. Mapping 409 duoc kiem rieng (deterministic) o GlobalExceptionHandlerTest.
        assertThat(statuses).filteredOn(s -> s == 200).hasSize(1);
        assertThat(statuses).filteredOn(s -> s != 200).singleElement().isIn(400, 409);

        MockHttpServletResponse loser = responses.stream()
                .filter(r -> r.getStatus() != 200).findFirst().orElseThrow();
        assertThat(loser.getContentType()).contains(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    }

    /**
     * (3) IDEMPOTENCY (tuan tu): cung Idempotency-Key + cung body -> tra CUNG order id, khong tao don
     * moi, va kho chi bi tru MOT lan (IdempotencyService tra ket qua cu tu Redis).
     */
    @Test
    void cungIdempotencyKey_traLaiDonCu_khoTruMotLan() throws Exception {
        Product p = productRepository.save(
                new Product("Hang idem HTTP", "du kho", new BigDecimal("1000"), 5, null));
        Long productId = p.getId();
        String key = "idem-" + productId;
        String body = "{\"items\":[{\"productId\":" + productId + ",\"quantity\":2}]}";

        String first = mockMvc.perform(post("/api/orders")
                        .with(user("customer").roles("CUSTOMER"))
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        Integer firstId = JsonPath.read(first, "$.id");

        String second = mockMvc.perform(post("/api/orders")
                        .with(user("customer").roles("CUSTOMER"))
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        Integer secondId = JsonPath.read(second, "$.id");

        // Cung id -> khong tao don trung; kho chi giam 2 (mot lan), khong phai 4.
        assertThat(secondId).isEqualTo(firstId);
        assertThat(productRepository.findById(productId).orElseThrow().getStockQuantity()).isEqualTo(3);
    }

    /** Chay <count> tac vu dong thoi, dong bo xuat phat bang CountDownLatch, tra ve ket qua moi tac vu. */
    private <T> List<T> runConcurrently(int count, Callable<T> task) throws Exception {
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(count);
        try {
            List<Future<T>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < count; i++) {
                futures.add(pool.submit(() -> {
                    startGate.await();
                    return task.call();
                }));
            }
            startGate.countDown();
            List<T> results = new java.util.ArrayList<>();
            for (Future<T> f : futures) {
                results.add(f.get(30, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }
}
