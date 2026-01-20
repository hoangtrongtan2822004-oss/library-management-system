package com.ibizabroker.lms.entity;

/**
 * 💰 Fine Status Enum
 * 
 * Trạng thái thanh toán phạt - Type-safe, tránh lỗi chính tả
 * 
 * 📌 States:
 * - UNPAID: Chưa thanh toán
 * - PAID: Đã thanh toán
 * - WAIVED: Được miễn phạt (admin có thể miễn cho các trường hợp đặc biệt)
 * - PARTIALLY_PAID: Thanh toán một phần (nếu cần)
 */
public enum FineStatus {
    /**
     * Chưa thanh toán - Mặc định khi có phạt
     */
    UNPAID,
    
    /**
     * Đã thanh toán đầy đủ
     */
    PAID,
    
    /**
     * Được miễn phạt (admin quyết định)
     */
    WAIVED,
    
    /**
     * Thanh toán một phần (optional - cho future enhancement)
     */
    PARTIALLY_PAID, NO_FINE
}
