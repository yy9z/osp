package com.caspar.entity.enums;

public enum GoodsStatus {

    PENDING("待审核"),

    APPROVED("已上架"),

    REJECTED("审核拒绝"),

    REMOVED("已下架"),

    SOLD("已售出");

    private final String description;

    GoodsStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
