package net.fill1890.fabsit.keybind;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fill1890.fabsit.FabSit;
import net.fill1890.fabsit.entity.Pose;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.Identifier;
import net.yukulab.fabpose.network.packet.play.PoseRequestC2SPacket;
import org.jetbrains.annotations.VisibleForTesting;

public abstract class PoseKeybinds {
    // translation keys for controls screen
    private static final String KEY = "key." + FabSit.MOD_ID + ".";
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(FabSit.MOD_ID, "category"));

    // sit, lay, and spin hotkeys
    @VisibleForTesting
    public static final KeyMapping sitKey = emptyKey("sit");
    @VisibleForTesting
    public static final KeyMapping layKey = emptyKey("lay");
    @VisibleForTesting
    public static final KeyMapping spinKey = emptyKey("spin");
    @VisibleForTesting
    public static final KeyMapping swimKey = emptyKey("swim");

    private static KeyMapping emptyKey(String base) {
        return KeyBindingHelper.registerKeyBinding(
                new KeyMapping(KEY + base, InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY)
        );
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while(sitKey.consumeClick()) {
                PoseRequestC2SPacket.send(Pose.SITTING);
            }

            while(layKey.consumeClick()) {
                PoseRequestC2SPacket.send(Pose.LAYING);
            }

            while(spinKey.consumeClick()) {
                PoseRequestC2SPacket.send(Pose.SPINNING);
            }
            while (swimKey.consumeClick()) {
                PoseRequestC2SPacket.send(Pose.SWIMMING);
            }
        });
    }
}
