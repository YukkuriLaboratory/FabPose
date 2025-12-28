package net.yukulab.fabpose.extension

import net.fill1890.fabsit.mixin.accessor.ScreenAccessor
import net.minecraft.client.gui.screens.Screen

val Screen.accessor: ScreenAccessor get() = this as ScreenAccessor
