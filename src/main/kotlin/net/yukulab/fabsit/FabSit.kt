package net.yukulab.fabsit

import net.fabricmc.api.ModInitializer
import net.yukulab.fabsit.command.Command

class FabSit : ModInitializer {
    override fun onInitialize() {
        Command.register()
    }
}
