package net.yukulab.fabpose.extension

import net.fill1890.fabsit.mixin.accessor.CreateWorldScreenAccessor
import net.minecraft.client.gui.screen.world.CreateWorldScreen

val CreateWorldScreen.accessor: CreateWorldScreenAccessor get() = this as CreateWorldScreenAccessor
