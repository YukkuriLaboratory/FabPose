package net.yukulab.fabpose.extension

import java.time.Instant
import net.fill1890.fabsit.entity.Pose
import net.fill1890.fabsit.extension.PosingFlag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player

var Player.currentPose: Pose?
    get() = (this as PosingFlag).`fabSit$currentPose`()
    set(value) = (this as PosingFlag).`fabSit$setPosing`(value)

val ServerPlayer.lastPoseTime: Instant?
    get() = (this as PosingFlag).`fabSit$lastPoseTime`()
