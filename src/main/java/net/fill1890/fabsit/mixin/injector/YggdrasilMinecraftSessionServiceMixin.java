package net.fill1890.fabsit.mixin.injector;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(YggdrasilMinecraftSessionService.class)
public abstract class YggdrasilMinecraftSessionServiceMixin {
    @Redirect(
            method = "getTextures",
            at = @At(value = "INVOKE", target = "Lcom/mojang/authlib/yggdrasil/YggdrasilMinecraftSessionService;getSecurePropertyValue(Lcom/mojang/authlib/properties/Property;)Ljava/lang/String;"),
            remap = false
    )
    private String ignoreSecureMode(YggdrasilMinecraftSessionService instance, Property property, @Local Property textureProperty) {
        return textureProperty.getValue();
    }
}
