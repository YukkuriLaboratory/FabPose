package net.fill1890.fabsit.extension;

public interface ForceSwimFlag {
    default boolean fabSit$shouldForceSwim() {
        return false;
    }

    default void fabSit$setForceSwim(boolean shouldSwim) {
    }
}
