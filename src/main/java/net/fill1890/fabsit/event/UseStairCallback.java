package net.fill1890.fabsit.event;

import net.fill1890.fabsit.config.ConfigManager;
import net.fill1890.fabsit.entity.ChairPosition;
import net.fill1890.fabsit.entity.Pose;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.yukulab.fabpose.entity.define.PoseManagerEntity;
import net.yukulab.fabpose.extension.ServerPlayerEntityKt;

public class UseStairCallback {
    public static InteractionResult interact(Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
        if (!(player instanceof ServerPlayer)) return InteractionResult.PASS;
        // only allow interaction if enabled
        if(!ConfigManager.getConfig().right_click_sit)
            return InteractionResult.PASS;

        // check player isn't spectating, sneaking, currently riding
        if(player.isSpectator())
            return InteractionResult.PASS;
        if(player.isShiftKeyDown())
            return InteractionResult.PASS;
        if(player.isPassenger())
            return InteractionResult.PASS;

        // player needs to click on an up-facing face
        if(hitResult.getDirection() != Direction.UP)
            return InteractionResult.PASS;

        BlockPos pos = hitResult.getBlockPos();
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        // check block is stair or slab
        if(!(block instanceof SlabBlock || block instanceof StairBlock))
            return InteractionResult.PASS;

        // use the block occupation logic since this forces centering
        if (PoseManagerEntity.isOccupied(world, pos))
            return InteractionResult.PASS;

        // player needs to click with an empty hand
        if(!player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty())
            return InteractionResult.PASS;

        // bottom slab only
        if(block instanceof SlabBlock && !isBottomSlab(state))
            return InteractionResult.PASS;

        // bottom stair only
        if(block instanceof StairBlock && !isBottomStair(state))
            return InteractionResult.PASS;

        // check block above is empty
        if(!world.getBlockState(pos.above()).isAir())
            return InteractionResult.PASS;

        // nice looking position
        Vec3 sitPos = new Vec3(pos.getX() + 0.5, pos.getY() + 0.4d, pos.getZ() + 0.5d);

        // tweak block position for stairs
        if (block instanceof StairBlock) {
            sitPos = sitPos.add(switch (state.getValue(StairBlock.FACING)) {
                case EAST -> new Vec3(-0.1, 0, 0);
                case SOUTH -> new Vec3(0, 0, -0.1);
                case WEST -> new Vec3(0.1, 0, 0);
                case NORTH -> new Vec3(0, 0, 0.1);
                default -> throw new IllegalStateException("Unexpected value: " + state.getValue(StairBlock.FACING));
            });
        }

        // set up the seat
        ServerPlayerEntityKt.pose((ServerPlayer) player, Pose.SITTING, sitPos, ChairPosition.IN_BLOCK);

        return InteractionResult.PASS;
    }

    private static boolean isBottomSlab(BlockState state) {
        return state.getProperties().contains(SlabBlock.TYPE) && state.getValue(SlabBlock.TYPE) == SlabType.BOTTOM;
    }

    private static boolean isBottomStair(BlockState state) {
        return state.getProperties().contains(StairBlock.HALF) && state.getValue(StairBlock.HALF) == Half.BOTTOM;
    }
}
