package com.learn.shopapi.config;

import com.learn.shopapi.entity.*;
import com.learn.shopapi.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Chay MOT LAN khi app khoi dong (chi o moi truong KHONG phai postgres - vi voi Postgres
 * thi Flyway lo viec tao bang + seed). Nap role/permission, tai khoan mau, san pham,
 * khach hang va vai don hang de cac bao cao co du lieu that.
 *
 * @Profile("!postgres"): bo qua khi chay voi profile postgres.
 */
@Component
@Profile("!postgres")
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepo;
    private final ProductRepository productRepo;
    private final CustomerRepository customerRepo;
    private final OrderRepository orderRepo;
    private final RoleRepository roleRepo;
    private final PermissionRepository permissionRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(CategoryRepository categoryRepo, ProductRepository productRepo,
                      CustomerRepository customerRepo, OrderRepository orderRepo,
                      RoleRepository roleRepo, PermissionRepository permissionRepo,
                      UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.categoryRepo = categoryRepo;
        this.productRepo = productRepo;
        this.customerRepo = customerRepo;
        this.orderRepo = orderRepo;
        this.roleRepo = roleRepo;
        this.permissionRepo = permissionRepo;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 0. Phan quyen: Permission -> Role -> User
        Permission productWrite = permissionRepo.save(new Permission("PRODUCT_WRITE"));
        Permission orderReadAll = permissionRepo.save(new Permission("ORDER_READ_ALL"));
        Permission orderUpdate = permissionRepo.save(new Permission("ORDER_UPDATE"));
        Permission reportView = permissionRepo.save(new Permission("REPORT_VIEW"));
        Permission userManage = permissionRepo.save(new Permission("USER_MANAGE"));

        Role adminRole = new Role("ADMIN");
        adminRole.setPermissions(Set.of(productWrite, orderReadAll, orderUpdate, reportView, userManage));
        roleRepo.save(adminRole);

        Role staffRole = new Role("STAFF");
        staffRole.setPermissions(Set.of(productWrite, orderReadAll, orderUpdate, reportView));
        roleRepo.save(staffRole);

        Role customerRole = roleRepo.save(new Role("CUSTOMER"));

        // Tai khoan mau (mat khau luu dang BCrypt). Dung de dang nhap thu ngay.
        User admin = new User("admin", passwordEncoder.encode("admin123"), "admin@example.com", "Quan tri vien");
        admin.addRole(adminRole);
        userRepo.save(admin);

        User staff = new User("staff", passwordEncoder.encode("staff123"), "staff@example.com", "Nhan vien");
        staff.addRole(staffRole);
        userRepo.save(staff);

        User customerUser = new User("customer", passwordEncoder.encode("customer123"), "an@example.com", "Nguyen Van An");
        customerUser.addRole(customerRole);
        userRepo.save(customerUser);

        // 1. Danh muc
        Category phone = categoryRepo.save(new Category("Dien thoai"));
        Category laptop = categoryRepo.save(new Category("Laptop"));
        Category book = categoryRepo.save(new Category("Sach"));

        // 2. San pham
        Product iphone = productRepo.save(new Product("iPhone 15", "Smartphone Apple",
                new BigDecimal("24990000"), 50, phone));
        Product samsung = productRepo.save(new Product("Samsung S24", "Smartphone Samsung",
                new BigDecimal("19990000"), 40, phone));
        Product mac = productRepo.save(new Product("MacBook Air M3", "Laptop Apple",
                new BigDecimal("28990000"), 20, laptop));
        Product cleanCode = productRepo.save(new Product("Clean Code", "Sach lap trinh",
                new BigDecimal("350000"), 100, book));

        // 3. Khach hang. "an" gan voi tai khoan "customer" -> dung de thu phan quyen "chi xem don cua minh".
        Customer an = customerRepo.save(new Customer("Nguyen Van An", "an@example.com", customerUser));
        Customer binh = customerRepo.save(new Customer("Tran Thi Binh", "binh@example.com"));

        // 4. Don hang mau
        Order o1 = new Order(an);
        o1.addItem(new OrderItem(iphone, 1));
        o1.addItem(new OrderItem(cleanCode, 2));
        o1.setStatus(OrderStatus.PAID);
        orderRepo.save(o1);

        Order o2 = new Order(binh);
        o2.addItem(new OrderItem(mac, 1));
        o2.addItem(new OrderItem(samsung, 1));
        o2.setStatus(OrderStatus.SHIPPED);
        orderRepo.save(o2);

        Order o3 = new Order(an);
        o3.addItem(new OrderItem(cleanCode, 3));
        o3.setStatus(OrderStatus.PAID);
        orderRepo.save(o3);

        // Don bi huy -> KHONG duoc tinh vao doanh thu (de kiem chung bao cao)
        Order o4 = new Order(binh);
        o4.addItem(new OrderItem(iphone, 1));
        o4.setStatus(OrderStatus.CANCELLED);
        orderRepo.save(o4);

        System.out.println("""

                =====================================================
                  DU LIEU MAU DA NAP XONG!
                  San pham: %d | Khach hang: %d | Don hang: %d
                  Tai khoan thu (username / password):
                    admin    / admin123     (ADMIN)
                    staff    / staff123     (STAFF)
                    customer / customer123  (CUSTOMER)
                  Dang nhap: POST http://localhost:8080/api/auth/login
                  Swagger  : http://localhost:8080/swagger-ui.html
                  H2 DB    : http://localhost:8080/h2-console
                =====================================================
                """.formatted(productRepo.count(), customerRepo.count(), orderRepo.count()));
    }
}
