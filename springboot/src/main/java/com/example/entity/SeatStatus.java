package com.example.entity;

public enum SeatStatus {
    AVAILABLE(0, "可预约"),
    RESERVED(1, "已预约"),
    IN_USE(2, "使用中"),
    MAINTENANCE(3, "维护中");

    private final int code;
    private final String description;

    SeatStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static SeatStatus fromCode(int code) {
        for (SeatStatus status : SeatStatus.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid SeatStatus code: " + code);
    }
}