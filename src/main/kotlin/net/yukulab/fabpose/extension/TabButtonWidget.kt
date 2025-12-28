package net.yukulab.fabpose.extension

import net.fill1890.fabsit.mixin.accessor.TabButtonWidgetAccessor
import net.minecraft.client.gui.components.TabButton

val TabButton.accessor: TabButtonWidgetAccessor get() = this as TabButtonWidgetAccessor
