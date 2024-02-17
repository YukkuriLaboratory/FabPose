package net.yukulab.fabpose.extension

import net.fill1890.fabsit.mixin.accessor.CyclingButtonWidgetAccessor
import net.minecraft.client.gui.widget.CyclingButtonWidget

val CyclingButtonWidget<*>.accessor: CyclingButtonWidgetAccessor get() = this as CyclingButtonWidgetAccessor
