package com.learn.shopapi.dto;

import com.learn.shopapi.entity.Cart;
import com.learn.shopapi.entity.CartItem;
import com.learn.shopapi.entity.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiem tra logic tinh tong tien gio hang trong CartResponse.from(Cart) ma CartService uy thac.
 * Dung BigDecimal.compareTo de so sanh tien (khong phu thuoc scale).
 */
class CartResponseTest {

    private CartItem item(String name, String price, int qty) {
        Product p = new Product(name, name + " desc", new BigDecimal(price), 100, null);
        return new CartItem(p, qty);
    }

    @Test
    void sumsMultipleLines() {
        Cart cart = new Cart(null);
        cart.addItem(item("A", "19.99", 3));
        cart.addItem(item("B", "5.50", 2));

        CartResponse r = CartResponse.from(cart);

        assertThat(r.items()).hasSize(2);

        CartResponse.Line a = r.items().get(0);
        assertThat(a.quantity()).isEqualTo(3);
        assertThat(a.unitPrice()).isEqualByComparingTo("19.99");
        assertThat(a.lineTotal()).isEqualByComparingTo("59.97");

        CartResponse.Line b = r.items().get(1);
        assertThat(b.quantity()).isEqualTo(2);
        assertThat(b.unitPrice()).isEqualByComparingTo("5.50");
        assertThat(b.lineTotal()).isEqualByComparingTo("11.00");

        assertThat(r.totalAmount().compareTo(new BigDecimal("70.97"))).isZero();
    }

    @Test
    void emptyCartHasNoItemsAndZeroTotal() {
        CartResponse r = CartResponse.from(new Cart(null));

        assertThat(r.items()).isEmpty();
        assertThat(r.totalAmount().compareTo(BigDecimal.ZERO)).isZero();
    }

    @Test
    void preservesCurrencyScale() {
        Cart cart = new Cart(null);
        cart.addItem(item("A", "10.00", 2));

        CartResponse r = CartResponse.from(cart);

        assertThat(r.items().get(0).lineTotal().scale()).isEqualTo(2);
        assertThat(r.totalAmount().scale()).isEqualTo(2);
        assertThat(r.totalAmount().compareTo(new BigDecimal("20.00"))).isZero();
    }
}
