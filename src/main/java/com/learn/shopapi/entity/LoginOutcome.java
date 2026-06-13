package com.learn.shopapi.entity;

/** Ket qua mot lan dang nhap (de luu vet bao mat). */
public enum LoginOutcome {
    SUCCESS,   // dang nhap thanh cong
    FAILURE,   // sai mat khau / tai khoan
    BLOCKED    // bi chan do dang nhap sai qua nhieu lan
}
