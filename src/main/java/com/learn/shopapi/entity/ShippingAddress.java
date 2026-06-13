package com.learn.shopapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Dia chi giao hang CHUP vao don luc tao (embeddable -> map thang vao cac cot cua bang orders,
 * tien to ship_). Chup tai thoi diem dat de lich su don khong doi khi khach sua dia chi sau nay.
 */
@Embeddable
public class ShippingAddress {

    @Column(name = "ship_recipient")
    private String recipient;

    @Column(name = "ship_phone")
    private String phone;

    @Column(name = "ship_line1")
    private String line1;

    @Column(name = "ship_line2")
    private String line2;

    @Column(name = "ship_city")
    private String city;

    @Column(name = "ship_province")
    private String province;

    @Column(name = "ship_postal_code")
    private String postalCode;

    @Column(name = "ship_country")
    private String country;

    protected ShippingAddress() { }

    public ShippingAddress(String recipient, String phone, String line1, String line2,
                           String city, String province, String postalCode, String country) {
        this.recipient = recipient;
        this.phone = phone;
        this.line1 = line1;
        this.line2 = line2;
        this.city = city;
        this.province = province;
        this.postalCode = postalCode;
        this.country = country;
    }

    public String getRecipient() { return recipient; }
    public String getPhone() { return phone; }
    public String getLine1() { return line1; }
    public String getLine2() { return line2; }
    public String getCity() { return city; }
    public String getProvince() { return province; }
    public String getPostalCode() { return postalCode; }
    public String getCountry() { return country; }
}
