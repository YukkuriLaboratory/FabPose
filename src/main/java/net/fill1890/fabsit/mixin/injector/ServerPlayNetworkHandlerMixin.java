package net.fill1890.fabsit.mixin.injector;

import io.netty.channel.ChannelFutureListener;
import net.fill1890.fabsit.mixin.accessor.EntitySpawnPacketAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.yukulab.fabpose.entity.FabSitEntities;
import net.yukulab.fabpose.entity.define.PoseManagerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hijack the network handler for various reasons
 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin extends ServerCommonNetworkHandler {
    @Shadow public ServerPlayerEntity player;

    public ServerPlayNetworkHandlerMixin(MinecraftServer server, ClientConnection connection, ConnectedClientData clientData) {
        super(server, connection, clientData);
    }

    /**
     * Listen for player hand swings
     * <br>
     * If the player is currently posing and has a posing NPC, transmit a swing packet to nearby players
     * <br>
     * @param packet passed from mixin function
     * @param ci mixin callback info
     */
    @Inject(method = "onHandSwing", at = @At("HEAD"))
    private void copyHandSwing(HandSwingC2SPacket packet, CallbackInfo ci) {
        // if player is currently posing
        if(this.player.hasVehicle() && this.player.getVehicle() instanceof PoseManagerEntity poseManager) {
            // animate if need be
            poseManager.animate(switch (packet.getHand()) {
                case MAIN_HAND -> EntityAnimationS2CPacket.SWING_MAIN_HAND;
                case OFF_HAND -> EntityAnimationS2CPacket.SWING_OFF_HAND;
            });
        }
    }

    /**
     * Hijack server -> client spawn packets and server -> client attribute updates
     * <br>
     * Spawn packets: If the server is trying to spawn a pose manager, overwrite with either an armor stand or a chair
     * depending on whether the client has fabsit loaded
     * <br>
     * Attribute updates: If the client has fabsit, error will be dumped in logs if we try to apply armor stand
     * attributes to a non-living entity, so block them
     *
     * @param packet passed from mixin function
     * @param callbacks passed from mixin function
     */
    @Override
    public void send(Packet<?> packet, @Nullable ChannelFutureListener callbacks) {
        // check for spawn packets, then spawn packets for the poser
        if (packet instanceof EntitySpawnS2CPacket sp) {
            fabPose$modifySpawnPacket(sp);
        } else if (packet instanceof BundleS2CPacket bp) {
            for (Packet<? super ClientPlayPacketListener> p : bp.getPackets()) {
                if (p instanceof EntitySpawnS2CPacket sp) {
                    fabPose$modifySpawnPacket(sp);
                }
            }
        }

        // check for entity attribute packets, and block for clients with fabsit
        // clients spit an error into logs when we try to update a non-living entity with living attributes
        if (packet instanceof EntityAttributesS2CPacket ap) {
            Entity entity = player.getWorld().getEntityById(ap.getEntityId());
            if (entity == null) {
                super.send(packet, callbacks);
                return;
            }

            EntityType<?> type = entity.getType();
            if (type != FabSitEntities.POSE_MANAGER) {
                super.send(packet, callbacks);
                return;
            }

            // cancel packet if player has fabsit loaded
            if (connection.fabSit$isModEnabled()) {
                return;
            }
        }
        super.send(packet, callbacks);
    }

    @Unique
    private void fabPose$modifySpawnPacket(EntitySpawnS2CPacket sp) {
        if (sp.getEntityType() == FabSitEntities.POSE_MANAGER) {

            // if fabsit not loaded, replace PoseManager entity to vanilla ArmorStand
            if (!connection.fabSit$isModEnabled()) {
                ((EntitySpawnPacketAccessor) sp).setEntityTypeId(EntityType.ARMOR_STAND);
            }
        }
    }
}
