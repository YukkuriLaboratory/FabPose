package net.fill1890.fabsit.mixin.injector;

import net.fill1890.fabsit.extension.ModFlag;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin implements ModFlag {
    @Unique
    boolean fabSit$isModEnabled = false;

    @Override
    public boolean fabSit$isModEnabled() {
        return fabSit$isModEnabled;
    }

    @Override
    public void fabSit$onModEnabled() {
        fabSit$isModEnabled = true;
    }
}
