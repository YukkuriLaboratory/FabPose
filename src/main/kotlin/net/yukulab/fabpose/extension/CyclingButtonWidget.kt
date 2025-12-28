package net.yukulab.fabpose.extension

import net.fill1890.fabsit.mixin.accessor.CycleButtonAccessor
import net.minecraft.client.gui.components.CycleButton

val CycleButton<*>.accessor: CycleButtonAccessor get() = this as CycleButtonAccessor
