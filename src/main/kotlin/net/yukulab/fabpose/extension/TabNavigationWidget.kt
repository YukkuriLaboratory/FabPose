package net.yukulab.fabpose.extension

import net.fill1890.fabsit.mixin.accessor.TabButtonWidgetAccessor
import net.minecraft.client.gui.components.tabs.TabNavigationBar

val TabNavigationBar.accessor: TabButtonWidgetAccessor get() = this as TabButtonWidgetAccessor
