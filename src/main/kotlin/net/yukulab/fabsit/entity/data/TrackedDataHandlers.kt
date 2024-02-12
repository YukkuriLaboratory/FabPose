package net.yukulab.fabsit.entity.data

import java.util.Optional
import net.fill1890.fabsit.entity.Pose
import net.minecraft.entity.data.TrackedDataHandler
import net.minecraft.entity.data.TrackedDataHandlerRegistry

object TrackedDataHandlers {
    @JvmField
    val POSE_HANDLER: TrackedDataHandler<Optional<Pose>> = TrackedDataHandler.of(
        { buf, pose ->
            val ordinal = pose.map { it.ordinal }.orElse(-1)
            buf.writeVarInt(ordinal)
        },
        {
            val ordinal = it.readVarInt()
            if (ordinal == -1) {
                Optional.empty()
            } else {
                Optional.of(Pose.entries[ordinal])
            }
        },
    )

    init {
        TrackedDataHandlerRegistry.register(POSE_HANDLER)
    }
}
