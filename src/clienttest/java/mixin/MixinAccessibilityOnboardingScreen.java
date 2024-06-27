package mixin;

import net.minecraft.client.gui.screen.AccessibilityOnboardingScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AccessibilityOnboardingScreen.class)
public abstract class MixinAccessibilityOnboardingScreen {
    @Inject(
            method = "saveAndRun",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/text2speech/Narrator;getNarrator()Lcom/mojang/text2speech/Narrator;"
            ),
            cancellable = true
    )
    private void suppressNarratorError(boolean dontShowAgain, Runnable callback, CallbackInfo ci) {
        callback.run();
        ci.cancel();
    }
}
