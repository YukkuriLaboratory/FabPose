package mixin;

import net.minecraft.client.gui.screen.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AccessibilityOnboardingScreen.class)
public abstract class MixinAccessibilityOnboardingScreen extends Screen {
    protected MixinAccessibilityOnboardingScreen(Text title) {
        super(title);
    }

    @Inject(
            method = "setScreen",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/text2speech/Narrator;getNarrator()Lcom/mojang/text2speech/Narrator;"
            ),
            cancellable = true
    )
    private void suppressNarratorError(Screen screen, CallbackInfo ci) {
        client.setScreen(screen);
        ci.cancel();
    }
}
