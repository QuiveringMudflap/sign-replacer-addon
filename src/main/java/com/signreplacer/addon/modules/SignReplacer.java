package com.signreplacer.addon.modules;

import com.signreplacer.addon.SignReplacerAddon;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.SignBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SignReplacer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgText = settings.createGroup("Sign Text");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General Settings
    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("The range to scan for signs.")
        .defaultValue(5)
        .min(1)
        .max(6)
        .sliderMax(6)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay in ticks between actions.")
        .defaultValue(5)
        .min(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotate towards the sign when interacting.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlyDifferent = sgGeneral.add(new BoolSetting.Builder()
        .name("only-different")
        .description("Only replace signs that have different text than your custom text.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoWalk = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-walk")
        .description("Automatically walk towards signs that are out of reach.")
        .defaultValue(true)
        .build()
    );

    // Sign Text Settings
    private final Setting<String> line1 = sgText.add(new StringSetting.Builder()
        .name("line-1")
        .description("Text for line 1 of the sign.")
        .defaultValue("VOID SUPPLY SHOP")
        .build()
    );

    private final Setting<String> line2 = sgText.add(new StringSetting.Builder()
        .name("line-2")
        .description("Text for line 2 of the sign.")
        .defaultValue("KITS & GEAR")
        .build()
    );

    private final Setting<String> line3 = sgText.add(new StringSetting.Builder()
        .name("line-3")
        .description("Text for line 3 of the sign.")
        .defaultValue("LIGHTNING FAST")
        .build()
    );

    private final Setting<String> line4 = sgText.add(new StringSetting.Builder()
        .name("line-4")
        .description("Text for line 4 of the sign.")
        .defaultValue("-> .gg/shop2b2t")
        .build()
    );

    // Render Settings
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Render signs in range.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color for rendering.")
        .defaultValue(new SettingColor(255, 0, 0, 75))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color for rendering.")
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .build()
    );

    private final Setting<SettingColor> targetColor = sgRender.add(new ColorSetting.Builder()
        .name("target-color")
        .description("The color for the current target sign.")
        .defaultValue(new SettingColor(0, 255, 0, 200))
        .build()
    );

    // State
    private final List<BlockPos> signs = new ArrayList<>();
    private BlockPos currentTarget = null;
    private int tickTimer = 0;
    private State state = State.Scanning;
    private BlockPos placePos = null;
    private Direction placeDirection = null;
    private float miningProgress = 0;
    private BlockPos miningBlock = null;

    private enum State {
        Scanning,
        Walking,
        Breaking,
        WaitingForBreak,
        Placing,
        WaitingForPlace,
        Editing
    }

    public SignReplacer() {
        super(SignReplacerAddon.CATEGORY, "sign-replacer", "Automatically breaks and replaces signs with custom text.");
    }

    @Override
    public void onActivate() {
        signs.clear();
        currentTarget = null;
        tickTimer = 0;
        state = State.Scanning;
        placePos = null;
        placeDirection = null;
        miningProgress = 0;
        miningBlock = null;
    }

    @Override
    public void onDeactivate() {
        signs.clear();
        currentTarget = null;
        miningBlock = null;
        miningProgress = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        tickTimer++;

        switch (state) {
            case Scanning -> scanForSigns();
            case Walking -> walkToSign();
            case Breaking -> breakSign();
            case WaitingForBreak -> waitForBreak();
            case Placing -> placeSign();
            case WaitingForPlace -> waitForPlace();
            case Editing -> editSign();
        }
    }

    private void scanForSigns() {
        signs.clear();
        BlockPos playerPos = mc.player.getBlockPos();
        int r = range.get();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    BlockState blockState = mc.world.getBlockState(pos);

                    if (isSign(blockState)) {
                        if (onlyDifferent.get()) {
                            if (!hasMatchingText(pos)) {
                                signs.add(pos);
                            }
                        } else {
                            signs.add(pos);
                        }
                    }
                }
            }
        }

        // Sort by distance
        signs.sort(Comparator.comparingDouble(pos ->
            mc.player.getPos().squaredDistanceTo(Vec3d.ofCenter(pos))));

        if (!signs.isEmpty()) {
            currentTarget = signs.get(0);
            placePos = currentTarget;
            placeDirection = getSignDirection(mc.world.getBlockState(currentTarget));

            double distance = mc.player.getPos().distanceTo(Vec3d.ofCenter(currentTarget));
            
            if (distance > 4.5 && autoWalk.get()) {
                state = State.Walking;
            } else {
                state = State.Breaking;
            }
            tickTimer = 0;
        }
    }

    private void walkToSign() {
        if (currentTarget == null) {
            state = State.Scanning;
            return;
        }

        Vec3d targetVec = Vec3d.ofCenter(currentTarget);
        double distance = mc.player.getPos().distanceTo(targetVec);

        // If close enough, start breaking
        if (distance <= 4.0) {
            state = State.Breaking;
            tickTimer = 0;
            return;
        }

        // Calculate direction to walk
        Vec3d playerPos = mc.player.getPos();
        Vec3d direction = targetVec.subtract(playerPos).normalize();

        // Face the direction we're walking
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        mc.player.setYaw(yaw);
        mc.player.setPitch(0);

        // Move player towards sign
        double speed = 0.2;
        mc.player.setVelocity(direction.x * speed, mc.player.getVelocity().y, direction.z * speed);

        // Timeout - if we can't reach after 10 seconds, skip this sign
        if (tickTimer > 200) {
            signs.remove(currentTarget);
            currentTarget = null;
            state = State.Scanning;
        }
    }

    private boolean isSign(BlockState state) {
        return state.getBlock() instanceof SignBlock || state.getBlock() instanceof WallSignBlock;
    }

    private boolean hasMatchingText(BlockPos pos) {
        if (mc.world.getBlockEntity(pos) instanceof SignBlockEntity signEntity) {
            var frontText = signEntity.getFrontText();
            String[] customLines = {line1.get(), line2.get(), line3.get(), line4.get()};

            for (int i = 0; i < 4; i++) {
                String signLine = frontText.getMessage(i, false).getString();
                if (!signLine.equals(customLines[i])) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private Direction getSignDirection(BlockState state) {
        if (state.getBlock() instanceof WallSignBlock) {
            return state.get(WallSignBlock.FACING);
        }
        return Direction.NORTH;
    }

    private void breakSign() {
        if (currentTarget == null) {
            state = State.Scanning;
            return;
        }

        if (tickTimer < delay.get()) return;

        BlockState blockState = mc.world.getBlockState(currentTarget);
        if (!isSign(blockState)) {
            state = State.Placing;
            tickTimer = 0;
            miningProgress = 0;
            miningBlock = null;
            return;
        }

        // Look at the sign
        if (rotate.get()) {
            Rotations.rotate(Rotations.getYaw(currentTarget), Rotations.getPitch(currentTarget));
        }

        // Start or continue mining
        if (miningBlock == null || !miningBlock.equals(currentTarget)) {
            // Start mining
            miningBlock = currentTarget;
            miningProgress = 0;
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                currentTarget,
                Direction.UP
            ));
        }

        // Continue mining - simulate holding down
        mc.interactionManager.updateBlockBreakingProgress(currentTarget, Direction.UP);
        miningProgress += blockState.calcBlockBreakingDelta(mc.player, mc.world, currentTarget);

        // Swing arm for visual feedback
        mc.player.swingHand(Hand.MAIN_HAND);

        // Check if block should be broken
        if (miningProgress >= 1.0f) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                currentTarget,
                Direction.UP
            ));
            state = State.WaitingForBreak;
            tickTimer = 0;
            miningProgress = 0;
            miningBlock = null;
        }
    }

    private void waitForBreak() {
        if (currentTarget == null) {
            state = State.Scanning;
            return;
        }

        // Check if block is broken
        if (!isSign(mc.world.getBlockState(currentTarget))) {
            state = State.Placing;
            tickTimer = 0;
            return;
        }

        // Timeout - try breaking again
        if (tickTimer > 40) {
            state = State.Breaking;
            tickTimer = 0;
        }
    }

    private void placeSign() {
        if (placePos == null) {
            state = State.Scanning;
            return;
        }

        if (tickTimer < delay.get()) return;

        // Find sign in inventory
        FindItemResult signItem = findSign();
        if (!signItem.found()) {
            info("No signs found in inventory!");
            state = State.Scanning;
            currentTarget = null;
            return;
        }

        // Swap to sign
        if (signItem.isOffhand()) {
            // Already good
        } else if (signItem.isHotbar()) {
            InvUtils.swap(signItem.slot(), true);
        } else {
            InvUtils.move().from(signItem.slot()).toHotbar(mc.player.getInventory().selectedSlot);
            return;
        }

        // Find a valid place position
        BlockPos supportPos = findSupportBlock(placePos);
        if (supportPos == null) {
            info("No valid support block found for sign placement!");
            state = State.Scanning;
            currentTarget = null;
            return;
        }

        Direction placeSide = getPlaceSide(supportPos, placePos);
        if (placeSide == null) {
            state = State.Scanning;
            currentTarget = null;
            return;
        }

        if (rotate.get()) {
            Rotations.rotate(Rotations.getYaw(placePos), Rotations.getPitch(placePos));
        }

        // Place the sign
        Vec3d hitPos = Vec3d.ofCenter(supportPos).add(
            placeSide.getOffsetX() * 0.5,
            placeSide.getOffsetY() * 0.5,
            placeSide.getOffsetZ() * 0.5
        );

        BlockHitResult hitResult = new BlockHitResult(hitPos, placeSide, supportPos, false);

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);

        if (signItem.isHotbar() && !signItem.isOffhand()) {
            InvUtils.swapBack();
        }

        state = State.WaitingForPlace;
        tickTimer = 0;
    }

    private FindItemResult findSign() {
        return InvUtils.find(itemStack ->
            itemStack.getItem() == Items.OAK_SIGN ||
            itemStack.getItem() == Items.SPRUCE_SIGN ||
            itemStack.getItem() == Items.BIRCH_SIGN ||
            itemStack.getItem() == Items.JUNGLE_SIGN ||
            itemStack.getItem() == Items.ACACIA_SIGN ||
            itemStack.getItem() == Items.DARK_OAK_SIGN ||
            itemStack.getItem() == Items.MANGROVE_SIGN ||
            itemStack.getItem() == Items.CHERRY_SIGN ||
            itemStack.getItem() == Items.BAMBOO_SIGN ||
            itemStack.getItem() == Items.CRIMSON_SIGN ||
            itemStack.getItem() == Items.WARPED_SIGN
        );
    }

    private BlockPos findSupportBlock(BlockPos signPos) {
        // Check below for standing sign
        BlockPos below = signPos.down();
        if (mc.world.getBlockState(below).isSolidBlock(mc.world, below)) {
            return below;
        }

        // Check sides for wall sign
        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos side = signPos.offset(dir);
            if (mc.world.getBlockState(side).isSolidBlock(mc.world, side)) {
                return side;
            }
        }

        return below; // Default to below
    }

    private Direction getPlaceSide(BlockPos supportPos, BlockPos targetPos) {
        int dx = targetPos.getX() - supportPos.getX();
        int dy = targetPos.getY() - supportPos.getY();
        int dz = targetPos.getZ() - supportPos.getZ();

        if (dy == 1) return Direction.UP;
        if (dx == 1) return Direction.EAST;
        if (dx == -1) return Direction.WEST;
        if (dz == 1) return Direction.SOUTH;
        if (dz == -1) return Direction.NORTH;

        return Direction.UP;
    }

    private void waitForPlace() {
        // Check if sign edit screen opens
        if (mc.currentScreen instanceof SignEditScreen) {
            state = State.Editing;
            tickTimer = 0;
            return;
        }

        // Check if sign was placed
        if (placePos != null && isSign(mc.world.getBlockState(placePos))) {
            state = State.Editing;
            tickTimer = 0;
            return;
        }

        // Timeout
        if (tickTimer > 40) {
            state = State.Scanning;
            currentTarget = null;
        }
    }

    private void editSign() {
        if (tickTimer < 2) return;

        // If sign edit screen is open, send the text
        if (mc.currentScreen instanceof SignEditScreen) {
            // Send sign update packet
            if (placePos != null || currentTarget != null) {
                BlockPos pos = placePos != null ? placePos : currentTarget;

                mc.player.networkHandler.sendPacket(new UpdateSignC2SPacket(
                    pos,
                    true, // front
                    line1.get(),
                    line2.get(),
                    line3.get(),
                    line4.get()
                ));

                mc.currentScreen.close();
            }
        }

        // Move to next sign
        state = State.Scanning;
        currentTarget = null;
        placePos = null;
        tickTimer = 0;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get()) return;

        // Render all signs in range
        for (BlockPos pos : signs) {
            if (pos.equals(currentTarget)) continue;

            event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }

        // Render current target
        if (currentTarget != null) {
            event.renderer.box(currentTarget, targetColor.get(), targetColor.get(), ShapeMode.Both, 0);
        }
    }
}
