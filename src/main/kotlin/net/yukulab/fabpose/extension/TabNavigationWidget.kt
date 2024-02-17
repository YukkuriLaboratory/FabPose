package net.yukulab.fabpose.extension

import net.fill1890.fabsit.mixin.accessor.TabButtonWidgetAccessor
import net.minecraft.client.gui.widget.TabNavigationWidget

val TabNavigationWidget.accessor: TabButtonWidgetAccessor get() = this as TabButtonWidgetAccessor
