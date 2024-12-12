package net.yukulab.fabpose.extension

import net.fill1890.fabsit.entity.Pose
import net.yukulab.fabpose.MOD_ID

fun Pose.getStaticName(): String {
    var name = this.name.lowercase()
    if (name.contains(".*ing$".toRegex())) {
        // sitting -> sitt
        name = name.removeSuffix("ing")
        // sitt -> sit
        if (name[name.lastIndex] == name[name.lastIndex - 1]) {
            name = name.dropLast(1)
        }
    }
    return name
}

private const val PERMISSION_NAME = "$MOD_ID.commands"

fun Pose.getPermissionName(): String {
    val k = "$PERMISSION_NAME.${getStaticName()}"
    return k
}
