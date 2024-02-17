package net.yukulab.fabpose.extension

import net.fill1890.fabsit.mixin.accessor.ScreenAccessor
import net.minecraft.client.gui.screen.Screen

val Screen.accessor: ScreenAccessor get() = this as ScreenAccessor
