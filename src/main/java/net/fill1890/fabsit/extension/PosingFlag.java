package net.fill1890.fabsit.extension;

import net.fill1890.fabsit.entity.Pose;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

public interface PosingFlag {
    @Nullable
    default Pose fabSit$currentPose() {
        return null;
    }

    default void fabSit$setPosing(@Nullable Pose pose) {
    }

    @Nullable
    default Instant fabSit$lastPoseTime() {
        return null;
    }
}
