package com.caspar.common;

public enum UserRole {
    STUDENT("学生"),
    TEACHER("教师"),
    ADMIN("管理员"),
    DORM_MANAGER("宿管员");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static UserRole fromString(String value) {
        for (UserRole role : UserRole.values()) {
            if (role.name().equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Invalid role: " + value);
    }
}
