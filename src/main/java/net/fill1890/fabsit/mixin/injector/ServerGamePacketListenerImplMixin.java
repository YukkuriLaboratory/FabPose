package net.fill1890.fabsit.mixin.injector;

import io.netty.channel.ChannelFutureListener;
import net.fill1890.fabsit.mixin.accessor.EntitySpawnPacketAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
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
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin extends ServerCommonPacketListenerImpl {
    @Shadow public ServerPlayer player;

    public ServerGamePacketListenerImplMixin(MinecraftServer server, Connection connection, CommonListenerCookie clientData) {
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
    @Inject(method = "handleAnimate", at = @At("HEAD"))
    private void copyHandSwing(ServerboundSwingPacket packet, CallbackInfo ci) {
        // if player is currently posing
        if(this.player.isPassenger() && this.player.getVehicle() instanceof PoseManagerEntity poseManager) {
            // animate if need be
            poseManager.animate(switch (packet.getHand()) {
                case MAIN_HAND -> ClientboundAnimatePacket.SWING_MAIN_HAND;
                case OFF_HAND -> ClientboundAnimatePacket.SWING_OFF_HAND;
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
        if (packet instanceof ClientboundAddEntityPacket sp) {
            fabPose$modifySpawnPacket(sp);
        } else if (packet instanceof ClientboundBundlePacket bp) {
            for (Packet<? super ClientGamePacketListener> p : bp.subPackets()) {
                if (p instanceof ClientboundAddEntityPacket sp) {
                    fabPose$modifySpawnPacket(sp);
                }
            }
        }

        // check for entity attribute packets, and block for clients with fabsit
        // clients spit an error into logs when we try to update a non-living entity with living attributes
        if (packet instanceof ClientboundUpdateAttributesPacket ap) {
            Entity entity = player.level().getEntity(ap.getEntityId());
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
    private void fabPose$modifySpawnPacket(ClientboundAddEntityPacket sp) {
        if (sp.getType() == FabSitEntities.POSE_MANAGER) {

            // if fabsit not loaded, replace PoseManager entity to vanilla ArmorStand
            if (!connection.fabSit$isModEnabled()) {
                ((EntitySpawnPacketAccessor) sp).setEntityTypeId(EntityType.ARMOR_STAND);
            }
        }
    }
}
