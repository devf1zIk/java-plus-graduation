package ru.yandex.practicum.enums;

public enum EventState {

    PENDING("PENDING"),
    PUBLISHED("PUBLISHED"),
    CANCELED("CANCELED");

    private final String state;

    EventState(String state) {
        this.state = state;
    }

    public String toString() {
        return state;
    }
}
