package net.fill1890.fabsit.extension;

public interface ModFlag {
    default boolean fabSit$isModEnabled() {
        return false;
    }

    default void fabSit$onModEnabled() {
    }
}
