package com.example.todo.model;

public enum Priority {
    HIGH("高", "bg-danger"),
    MEDIUM("中", "bg-warning text-dark"),
    LOW("低", "bg-success");

    private final String label;
    private final String badgeClass;

    Priority(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}
