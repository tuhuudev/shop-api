package com.learn.shopapi.repository;

/**
 * Helper escape ky tu wildcard cho LIKE.
 *
 * Spring Data (Containing/StartingWith...) va CriteriaBuilder.like() deu KHONG tu escape
 * '%' va '_' trong gia tri nguoi dung nhap. Hau qua: tu khoa "50%" sinh ra LIKE '%50%%'
 * va khop MOI dong. Dung {@link #escape(String)} de coi nhung ky tu do la ky tu thuong,
 * roi truyen {@link #ESCAPE} lam escape char cho lenh LIKE.
 */
public final class LikePatterns {

    /** Escape char di kem moi lenh LIKE dung gia tri da escape o day. */
    public static final char ESCAPE = '\\';

    private LikePatterns() { }

    /**
     * Escape '\', '%', '_' de chung duoc coi la ky tu thuong trong LIKE.
     * Thu tu quan trong: escape chinh ky tu escape ('\') TRUOC, roi moi den '%' va '_'.
     * Tra ve null neu input null (de caller tu quyet dinh xu ly).
     */
    public static String escape(String input) {
        if (input == null) {
            return null;
        }
        return input
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
