package net.yukulab.fabsit.extension

import net.fill1890.fabsit.mixin.accessor.TabButtonWidgetAccessor
import net.minecraft.client.gui.widget.TabButtonWidget

val TabButtonWidget.accessor: TabButtonWidgetAccessor get() = this as TabButtonWidgetAccessor
