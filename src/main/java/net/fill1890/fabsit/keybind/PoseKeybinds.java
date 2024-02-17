package net.fill1890.fabsit.keybind;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fill1890.fabsit.FabSit;
import net.fill1890.fabsit.entity.Pose;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.yukulab.fabpose.network.packet.play.PoseRequestC2SPacket;
import org.jetbrains.annotations.VisibleForTesting;

public abstract class PoseKeybinds {
    // translation keys for controls screen
    private static final String KEY = "key." + FabSit.MOD_ID + ".";
    private static final String CATEGORY = "key." + FabSit.MOD_ID + ".category";

    // sit, lay, and spin hotkeys
    @VisibleForTesting
    public static final KeyBinding sitKey = emptyKey("sit");
    @VisibleForTesting
    public static final KeyBinding layKey = emptyKey("lay");
    @VisibleForTesting
    public static final KeyBinding spinKey = emptyKey("spin");
    @VisibleForTesting
    public static final KeyBinding swimKey = emptyKey("swim");

    private static KeyBinding emptyKey(String base) {
        return KeyBindingHelper.registerKeyBinding(
                new KeyBinding(KEY + base, InputUtil.Type.KEYSYM,InputUtil.UNKNOWN_KEY.getCode(), CATEGORY)
        );
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while(sitKey.wasPressed()) {
                PoseRequestC2SPacket.send(Pose.SITTING);
            }

            while(layKey.wasPressed()) {
                PoseRequestC2SPacket.send(Pose.LAYING);
            }

            while(spinKey.wasPressed()) {
                PoseRequestC2SPacket.send(Pose.SPINNING);
            }
            while (swimKey.wasPressed()) {
                PoseRequestC2SPacket.send(Pose.SWIMMING);
            }
        });
    }
}
