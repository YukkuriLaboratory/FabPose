package net.fill1890.fabsit.mixin.injector;

import net.fill1890.fabsit.extension.ModFlag;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Connection.class)
public abstract class ConnectionMixin implements ModFlag {
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
