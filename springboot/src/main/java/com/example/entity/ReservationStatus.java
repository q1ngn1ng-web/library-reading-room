package com.example.entity;

public enum ReservationStatus {
    SUCCESS(1, "成功"),
    FAILED(2, "失败"),
    TIMEOUT(3, "超时"),
    CANCELLED(4, "取消"),
    CHECKED_IN(5, "已签到"),
    COMPLETED(6, "已完成");

    private final int code;
    private final String description;

    ReservationStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ReservationStatus fromCode(int code) {
        for (ReservationStatus status : ReservationStatus.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid ReservationStatus code: " + code);
    }
}