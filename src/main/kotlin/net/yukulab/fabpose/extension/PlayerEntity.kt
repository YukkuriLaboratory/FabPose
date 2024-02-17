package net.yukulab.fabpose.extension

import java.time.Instant
import net.fill1890.fabsit.entity.Pose
import net.fill1890.fabsit.extension.PosingFlag
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.network.ServerPlayerEntity

var PlayerEntity.currentPose: Pose?
    get() = (this as PosingFlag).`fabSit$currentPose`()
    set(value) = (this as PosingFlag).`fabSit$setPosing`(value)

val ServerPlayerEntity.lastPoseTime: Instant?
    get() = (this as PosingFlag).`fabSit$lastPoseTime`()
