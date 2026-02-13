package com.signreplacer.addon.modules;

import com.signreplacer.addon.SignReplacerAddon;
import com.signreplacer.addon.baritone.BaritoneHelper;
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
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SignReplacer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgText = settings.createGroup("Sign Text");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General Settings
    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("The range to scan for signs (blocks in all directions).")
        .defaultValue(100)
        .min(1)
        .max(100)
        .sliderMax(100)
        .build()
    );

    private final Setting<Integer> giveUpTicks = sgGeneral.add(new IntSetting.Builder()
        .name("give-up-ticks")
        .description("If the current sign can't be mined or reached in this many ticks, skip it and move to the next. 20 ticks = 1 second. Prevents getting stuck on one sign forever.")
        .defaultValue(3000)
        .min(200)
        .max(12000)
        .sliderMax(6000)
        .build()
    );

    private final Setting<Double> pickupRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("pickup-range")
        .description("Walk to dropped sign within this many blocks before placing new sign.")
        .defaultValue(2.0)
        .min(1.0)
        .max(10.0)
        .decimalPlaces(1)
        .build()
    );

    private final Setting<Double> maxDropDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-drop-distance")
        .description("Give up chasing the dropped sign if it's farther than this (e.g. fell off cliff). Don't walk all the way down for one sign.")
        .defaultValue(15.0)
        .min(5.0)
        .max(50.0)
        .decimalPlaces(1)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay in ticks between actions. Lower = faster (aim for ~1 sign every 2 sec with 1–2).")
        .defaultValue(1)
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

    private final Setting<Boolean> useBaritone = sgGeneral.add(new BoolSetting.Builder()
        .name("use-baritone")
        .description("Use Baritone for pathfinding (install Baritone mod). Handles jumping, climbing, and looks like normal movement on 2b2t.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> placeOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("place-only")
        .description("Don't mine existing signs; only place new signs (look down and right-click periodically).")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> placePitch = sgGeneral.add(new DoubleSetting.Builder()
        .name("place-pitch")
        .description("Pitch (degrees) to look when placing in place-only mode. 90 = straight down.")
        .defaultValue(70.0)
        .min(30.0)
        .max(90.0)
        .decimalPlaces(1)
        .build()
    );

    private final Setting<Boolean> jumpWhenNeeded = sgGeneral.add(new BoolSetting.Builder()
        .name("jump-when-needed")
        .description("Jump when placing a sign one block above feet so you can reach it.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> placementCooldown = sgGeneral.add(new IntSetting.Builder()
        .name("placement-cooldown")
        .description("Ticks to wait after placing a sign before finding the next spot. Reduces lag spikes.")
        .defaultValue(10)
        .min(0)
        .max(40)
        .sliderMax(20)
        .build()
    );

    private final Setting<Integer> scanInterval = sgGeneral.add(new IntSetting.Builder()
        .name("scan-interval")
        .description("In place-only mode, only run scan every N ticks. Higher = less CPU, slightly slower placement.")
        .defaultValue(2)
        .min(1)
        .max(10)
        .build()
    );

    // Sign Text Settings
    private final Setting<String> line1 = sgText.add(new StringSetting.Builder()
        .name("line-1")
        .description("Text for line 1 of the sign.")
        .defaultValue("#1 FASTEST")
        .build()
    );

    private final Setting<String> line2 = sgText.add(new StringSetting.Builder()
        .name("line-2")
        .description("Text for line 2 of the sign.")
        .defaultValue("DELIVERY ON 2B2T")
        .build()
    );

    private final Setting<String> line3 = sgText.add(new StringSetting.Builder()
        .name("line-3")
        .description("Text for line 3 of the sign.")
        .defaultValue("KITS & GEAR")
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
    private int ticksAtTarget = 0;
    private int collectingTicks = 0;
    private State state = State.Scanning;
    private BlockPos placePos = null;
    private Direction placeDirection = null;
    private float miningProgress = 0;
    private BlockPos miningBlock = null;
    // Chunked scan for large range: max blocks per tick (higher = faster scan, small stutter)
    private static final int MAX_BLOCKS_PER_TICK = 8192;
    private int scanCurrentY = Integer.MIN_VALUE;
    private int scanCurrentX;
    private int scanCurrentZ;
    private int scanMinY;
    private int scanMaxY;
    private int scanMinX;
    private int scanMaxX;
    private int scanMinZ;
    private int scanMaxZ;
    private int placementCooldownTicks = 0;
    private int jumpReleaseTicks = 0;
    /** Consecutive ticks we've had a sign in inventory (avoid leaving drop phase before pickup is synced). */
    private int ticksWithSign = 0;
    /** Retries of "go back to drop" when we reach Placing without a sign (2b2t lag). */
    private int placeRetryCount = 0;
    /** Send sign text this many times (2b2t often drops the packet). */
    private int signTextSendsLeft = 0;
    private BlockPos signTextTargetPos = null;
    private static final int TICKS_REQUIRED_WITH_SIGN = 10;
    private static final int MAX_PLACE_RETRIES = 3;
    private static final int MAX_RENDER_BOXES = 64;
    /** Skip these positions when scanning for 20 sec so we don't re-mine signs we just placed. */
    private final Map<String, Long> recentlyPlaced = new HashMap<>();
    private static final long RECENTLY_PLACED_MS = 20_000;

    private enum State {
        Scanning,
        Walking,
        PathingToSign,
        PathingToDrop,
        PathingToPlace,
        Breaking,
        WaitingForBreak,
        CollectingItem,
        Placing,
        WaitingForPlace
    }

    public SignReplacer() {
        super(SignReplacerAddon.CATEGORY, "sign-replacer", "Automatically breaks and replaces signs with custom text.");
    }

    @Override
    public void onActivate() {
        signs.clear();
        currentTarget = null;
        tickTimer = 0;
        ticksAtTarget = 0;
        state = State.Scanning;
        placePos = null;
        placeDirection = null;
        miningProgress = 0;
        miningBlock = null;
        collectingTicks = 0;
        scanCurrentY = Integer.MIN_VALUE;
        placementCooldownTicks = 0;
        jumpReleaseTicks = 0;
        ticksWithSign = 0;
        placeRetryCount = 0;
        signTextSendsLeft = 0;
        signTextTargetPos = null;
    }

    @Override
    public void onDeactivate() {
        BaritoneHelper.cancelPath();
        signs.clear();
        currentTarget = null;
        miningBlock = null;
        miningProgress = 0;
        if (mc.options != null) {
            mc.options.forwardKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
        }
    }

    private static String posKey(BlockPos p) {
        return p.getX() + "," + p.getY() + "," + p.getZ();
    }

    private boolean isRecentlyPlaced(BlockPos pos) {
        long now = System.currentTimeMillis();
        recentlyPlaced.entrySet().removeIf(e -> now - e.getValue() > RECENTLY_PLACED_MS);
        return recentlyPlaced.containsKey(posKey(pos));
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (state == State.Breaking) {
            jumpReleaseTicks = 0;
            mc.options.jumpKey.setPressed(false);
        } else if (jumpReleaseTicks > 0) {
            jumpReleaseTicks--;
            if (jumpReleaseTicks == 0) {
                mc.options.jumpKey.setPressed(false);
            }
        }

        tickTimer++;

        switch (state) {
            case Scanning -> scanForSigns();
            case Walking -> walkToSign();
            case PathingToSign -> tickPathingToSign();
            case PathingToDrop -> tickPathingToDrop();
            case PathingToPlace -> tickPathingToPlace();
            case Breaking -> breakSign();
            case WaitingForBreak -> waitForBreak();
            case CollectingItem -> walkToDrop();
            case Placing -> placeSign();
            case WaitingForPlace -> waitForPlace();
        }
    }

    private void scanForSigns() {
        if (placeOnly.get()) {
            if (placementCooldownTicks > 0) {
                placementCooldownTicks--;
                return;
            }
            if (tickTimer % scanInterval.get() != 0) return;
            scanForPlaceOnly();
            return;
        }
        int r = range.get();
        if (r > 32) {
            scanForSignsLayered();
            return;
        }
        scanForSignsFull(r);
    }

    /** Chunked scan when range > 32: at most MAX_BLOCKS_PER_TICK per tick so no lag spike. */
    private void scanForSignsLayered() {
        BlockPos playerPos = mc.player.getBlockPos();
        int r = range.get();
        if (scanCurrentY == Integer.MIN_VALUE) {
            int worldMinY = -64;
            int worldMaxY = 319;
            scanMinY = Math.max(worldMinY, playerPos.getY() - r);
            scanMaxY = Math.min(worldMaxY, playerPos.getY() + r);
            scanMinX = playerPos.getX() - r;
            scanMaxX = playerPos.getX() + r;
            scanMinZ = playerPos.getZ() - r;
            scanMaxZ = playerPos.getZ() + r;
            scanCurrentY = scanMinY;
            scanCurrentX = scanMinX;
            scanCurrentZ = scanMinZ;
            signs.clear();
        }
        int done = 0;
        while (scanCurrentY <= scanMaxY && done < MAX_BLOCKS_PER_TICK) {
            while (scanCurrentX <= scanMaxX && done < MAX_BLOCKS_PER_TICK) {
                while (scanCurrentZ <= scanMaxZ && done < MAX_BLOCKS_PER_TICK) {
                    BlockPos pos = new BlockPos(scanCurrentX, scanCurrentY, scanCurrentZ);
                    if (!isRecentlyPlaced(pos)) {
                        BlockState blockState = mc.world.getBlockState(pos);
                        if (isSign(blockState)) {
                            if (onlyDifferent.get()) {
                                if (!hasMatchingText(pos)) signs.add(pos);
                            } else {
                                signs.add(pos);
                            }
                        }
                    }
                    done++;
                    scanCurrentZ++;
                }
                scanCurrentZ = scanMinZ;
                scanCurrentX++;
            }
            scanCurrentX = scanMinX;
            scanCurrentY++;
        }
        if (scanCurrentY > scanMaxY) {
            scanCurrentY = Integer.MIN_VALUE;
            finishScanAndPickTarget();
        }
    }

    /** Original full 3D scan when range <= 32. */
    private void scanForSignsFull(int r) {
        signs.clear();
        BlockPos playerPos = mc.player.getBlockPos();
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (isRecentlyPlaced(pos)) continue;
                    BlockState blockState = mc.world.getBlockState(pos);
                    if (isSign(blockState)) {
                        if (onlyDifferent.get()) {
                            if (!hasMatchingText(pos)) signs.add(pos);
                        } else {
                            signs.add(pos);
                        }
                    }
                }
            }
        }
        finishScanAndPickTarget();
    }

    private void finishScanAndPickTarget() {
        signs.sort(Comparator.comparingDouble(pos ->
            mc.player.getPos().squaredDistanceTo(Vec3d.ofCenter(pos))));
        if (signs.isEmpty()) return;
        currentTarget = signs.get(0);
        placePos = currentTarget;
        placeDirection = getSignDirection(mc.world.getBlockState(currentTarget));
        double distance = mc.player.getPos().distanceTo(Vec3d.ofCenter(currentTarget));
        if (distance > 4.5 && autoWalk.get()) {
            if (useBaritone.get() && BaritoneHelper.isAvailable()) {
                BaritoneHelper.pathTo(currentTarget);
                state = State.PathingToSign;
                ticksAtTarget = 0;
            } else {
                state = State.Walking;
            }
        } else {
            state = State.Breaking;
            jumpReleaseTicks = 0;
            mc.options.jumpKey.setPressed(false);
        }
        tickTimer = 0;
    }

    private void tickPathingToSign() {
        if (currentTarget == null) {
            state = State.Scanning;
            return;
        }
        ticksAtTarget++;
        if (ticksAtTarget > giveUpTicks.get()) {
            BaritoneHelper.cancelPath();
            signs.remove(currentTarget);
            currentTarget = null;
            state = State.Scanning;
            ticksAtTarget = 0;
            return;
        }
        if (BaritoneHelper.isAdjacentTo(currentTarget, mc.player.getBlockPos())) {
            BaritoneHelper.cancelPath();
            state = State.Breaking;
            jumpReleaseTicks = 0;
            mc.options.jumpKey.setPressed(false);
            tickTimer = 0;
        }
    }

    private void scanForPlaceOnly() {
        // Look down so we hit the ground in front
        float pitchDeg = (float) (double) placePitch.get();
        mc.player.setPitch(pitchDeg);
        Vec3d start = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVec(1.0f);
        Vec3d end = start.add(look.multiply(5.0));
        RaycastContext ctx = new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player);
        HitResult hit = mc.world.raycast(ctx);
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            // No block hit (sky/void) – turn a bit and try again next tick
            mc.player.setYaw(mc.player.getYaw() + 15f);
            return;
        }
        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockPos supportPos = blockHit.getBlockPos();
        Direction face = blockHit.getSide();
        BlockPos signPos = supportPos.offset(face);
        if (!mc.world.getBlockState(signPos).isAir()) {
            mc.player.setYaw(mc.player.getYaw() + 15f);
            return;
        }
        placePos = signPos;
        currentTarget = null;
        Vec3d targetVec = Vec3d.ofCenter(placePos);
        double distance = mc.player.getPos().distanceTo(targetVec);
        if (distance > 4.5 && autoWalk.get()) {
            if (useBaritone.get() && BaritoneHelper.isAvailable()) {
                BaritoneHelper.pathTo(placePos);
                state = State.PathingToPlace;
                ticksAtTarget = 0;
            } else {
                state = State.Walking;
                ticksAtTarget = 0;
            }
        } else {
            state = State.Placing;
        }
        tickTimer = 0;
    }

    private void tickPathingToPlace() {
        if (placePos == null) {
            state = State.Scanning;
            return;
        }
        ticksAtTarget++;
        if (ticksAtTarget > giveUpTicks.get()) {
            BaritoneHelper.cancelPath();
            placePos = null;
            state = State.Scanning;
            ticksAtTarget = 0;
            return;
        }
        if (BaritoneHelper.isAdjacentTo(placePos, mc.player.getBlockPos())) {
            BaritoneHelper.cancelPath();
            state = State.Placing;
            tickTimer = 0;
        }
    }

    private void tickPathingToDrop() {
        if (placePos == null) {
            state = State.Scanning;
            return;
        }
        collectingTicks++;
        ItemEntity dropEntity = findDroppedSignEntity(placePos);
        boolean haveSign = findSign().found();

        if (dropEntity == null || !dropEntity.isAlive()) {
            if (haveSign) {
                BaritoneHelper.cancelPath();
                collectingTicks = 0;
                ticksWithSign = TICKS_REQUIRED_WITH_SIGN;
                state = State.Placing;
                placePos = null;
                tickTimer = 0;
                placeRetryCount = 0;
            } else if (collectingTicks > 80) {
                BaritoneHelper.cancelPath();
                if (currentTarget != null) signs.remove(currentTarget);
                currentTarget = null;
                placePos = null;
                state = State.Scanning;
                collectingTicks = 0;
                ticksWithSign = 0;
            }
            return;
        }
        Vec3d dropVec = dropEntity.getPos();
        double distance = mc.player.getPos().distanceTo(dropVec);
        if (distance > maxDropDistance.get()) {
            BaritoneHelper.cancelPath();
            if (currentTarget != null) signs.remove(currentTarget);
            currentTarget = null;
            placePos = null;
            state = State.Scanning;
            collectingTicks = 0;
            ticksWithSign = 0;
            return;
        }
        if (collectingTicks % 15 == 0) {
            BaritoneHelper.pathTo(dropVec);
        }
        boolean inRange = distance <= pickupRange.get();
        if (haveSign) {
            ticksWithSign++;
        } else {
            ticksWithSign = 0;
        }
        if (inRange && ticksWithSign >= TICKS_REQUIRED_WITH_SIGN) {
            BaritoneHelper.cancelPath();
            collectingTicks = 0;
            placeRetryCount = 0;
            state = State.Placing;
            placePos = null;
            tickTimer = 0;
        }
        if (collectingTicks > 400 && haveSign) {
            BaritoneHelper.cancelPath();
            state = State.Placing;
            placePos = null;
            tickTimer = 0;
            placeRetryCount = 0;
            collectingTicks = 0;
        }
    }

    private void walkToSign() {
        BlockPos walkTarget = currentTarget != null ? currentTarget : placePos;
        if (walkTarget == null) {
            state = State.Scanning;
            return;
        }

        ticksAtTarget++;
        if (ticksAtTarget > giveUpTicks.get() && currentTarget != null) {
            signs.remove(currentTarget);
            currentTarget = null;
            state = State.Scanning;
            ticksAtTarget = 0;
            return;
        }
        if (ticksAtTarget > giveUpTicks.get() && placeOnly.get()) {
            placePos = null;
            state = State.Scanning;
            ticksAtTarget = 0;
            return;
        }

        Vec3d targetVec = Vec3d.ofCenter(walkTarget);
        double distance = mc.player.getPos().distanceTo(targetVec);

        if (distance <= 4.0) {
            if (placeOnly.get()) {
                state = State.Placing;
            } else {
                state = State.Breaking;
                jumpReleaseTicks = 0;
                mc.options.jumpKey.setPressed(false);
            }
            tickTimer = 0;
            return;
        }

        Vec3d playerPos = mc.player.getPos();
        Vec3d direction = targetVec.subtract(playerPos).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        mc.player.setYaw(yaw);
        mc.player.setPitch(placeOnly.get() ? (float) (double) placePitch.get() : 0);
        double speed = 0.2;
        mc.player.setVelocity(direction.x * speed, mc.player.getVelocity().y, direction.z * speed);
    }

    private boolean isSign(BlockState state) {
        return state.getBlock() instanceof SignBlock || state.getBlock() instanceof WallSignBlock;
    }

    private boolean hasMatchingText(BlockPos pos) {
        if (mc.world.getBlockEntity(pos) instanceof SignBlockEntity signEntity) {
            try {
                var frontText = signEntity.getFrontText();
                String[] customLines = {line1.get(), line2.get(), line3.get(), line4.get()};
                for (int i = 0; i < 4 && i < customLines.length; i++) {
                    String signLine = frontText.getMessage(i, false).getString();
                    if (!signLine.equals(customLines[i])) {
                        return false;
                    }
                }
                return true;
            } catch (IndexOutOfBoundsException e) {
                return false;
            }
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
        mc.options.jumpKey.setPressed(false);

        if (tickTimer < delay.get()) return;

        BlockState blockState = mc.world.getBlockState(currentTarget);
        if (!isSign(blockState)) {
            tickTimer = 0;
            miningProgress = 0;
            miningBlock = null;
            collectingTicks = 0;
            state = State.CollectingItem;
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

        if (!isSign(mc.world.getBlockState(currentTarget))) {
            tickTimer = 0;
            collectingTicks = 0;
            if (useBaritone.get() && BaritoneHelper.isAvailable() && placePos != null) {
                BaritoneHelper.pathTo(placePos);
                state = State.PathingToDrop;
            } else {
                state = State.CollectingItem;
            }
            return;
        }

        if (tickTimer > 15) {
            if (useBaritone.get() && BaritoneHelper.isAvailable() && placePos != null) {
                BaritoneHelper.pathTo(placePos);
                state = State.PathingToDrop;
            } else {
                state = State.CollectingItem;
            }
            tickTimer = 0;
            return;
        }
        if (tickTimer > 5) {
            state = State.Breaking;
            tickTimer = 0;
        }
    }

    private void walkToDrop() {
        if (placePos == null) {
            state = State.Placing;
            return;
        }

        collectingTicks++;
        // Walk to where the sign DROPPED (the item entity), NOT where the sign was broken (e.g. if it fell off cliff)
        ItemEntity dropEntity = findDroppedSignEntity(placePos);
        if (dropEntity == null || !dropEntity.isAlive()) {
            if (collectingTicks > 40) {
                if (currentTarget != null) signs.remove(currentTarget);
                currentTarget = null;
                placePos = null;
                state = State.Scanning;
                collectingTicks = 0;
                mc.options.jumpKey.setPressed(false);
            }
            return;
        }
        Vec3d dropVec = dropEntity.getPos();
        double distance = mc.player.getPos().distanceTo(dropVec);
        double maxDist = maxDropDistance.get();
        if (distance > maxDist) {
            if (currentTarget != null) signs.remove(currentTarget);
            currentTarget = null;
            placePos = null;
            state = State.Scanning;
            collectingTicks = 0;
            mc.options.jumpKey.setPressed(false);
            return;
        }

        // Only go to Placing when we're in range AND we have the sign (picked up)
        boolean inRange = distance <= pickupRange.get();
        boolean haveSign = findSign().found();
        if (inRange && haveSign) {
            state = State.Placing;
            tickTimer = 0;
            collectingTicks = 0;
            return;
        }
        if (collectingTicks > 200) {
            state = State.Placing;
            tickTimer = 0;
            collectingTicks = 0;
            return;
        }

        if (inRange) return;

        Vec3d playerPos = mc.player.getPos();
        Vec3d direction = dropVec.subtract(playerPos).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        mc.player.setYaw(yaw);
        mc.player.setPitch(0);
        double speed = 0.2;
        mc.player.setVelocity(direction.x * speed, mc.player.getVelocity().y, direction.z * speed);
        if (jumpWhenNeeded.get() && dropVec.y > mc.player.getY() + 0.6) {
            mc.options.jumpKey.setPressed(true);
        } else {
            mc.options.jumpKey.setPressed(false);
        }
    }

    /** Wait a few ticks after reaching place pos so inventory is synced (2b2t lag). */
    private static final int PLACE_WAIT_TICKS = 3;

    private void placeSign() {
        if (tickTimer < delay.get() + PLACE_WAIT_TICKS) return;

        FindItemResult signItem = findSign();
        if (!signItem.found()) {
            if (tickTimer > 80) {
                placeRetryCount++;
                if (placeRetryCount < MAX_PLACE_RETRIES) {
                    ticksWithSign = 0;
                    collectingTicks = 0;
                    state = State.PathingToDrop;
                    tickTimer = 0;
                } else {
                    state = State.Scanning;
                    currentTarget = null;
                    placePos = null;
                }
            }
            return;
        }

        if (signItem.isOffhand()) {
            // Already good
        } else if (signItem.isHotbar()) {
            InvUtils.swap(signItem.slot(), true);
        } else {
            InvUtils.move().from(signItem.slot()).toHotbar(mc.player.getInventory().selectedSlot);
            return;
        }

        BlockPos supportPos;
        BlockPos signPos;
        Direction placeSide;
        if (placePos != null) {
            supportPos = findSupportBlock(placePos);
            if (supportPos == null) {
                placePos = null;
                return;
            }
            signPos = placePos;
            placeSide = getPlaceSide(supportPos, placePos);
        } else {
            PlaceSpot spot = findPlaceSpotNearPlayer();
            if (spot == null) {
                mc.player.setYaw(mc.player.getYaw() + 25f);
                return;
            }
            supportPos = spot.supportPos;
            signPos = spot.signPos;
            placeSide = spot.side;
            placePos = signPos;
        }

        if (rotate.get()) {
            Rotations.rotate(Rotations.getYaw(signPos), Rotations.getPitch(signPos));
        }

        if (jumpWhenNeeded.get() && signPos.getY() > mc.player.getBlockY()) {
            mc.options.jumpKey.setPressed(true);
            jumpReleaseTicks = 8;
        }

        Vec3d faceCenter = Vec3d.ofCenter(supportPos).add(
            placeSide.getOffsetX() * 0.5,
            placeSide.getOffsetY() * 0.5,
            placeSide.getOffsetZ() * 0.5
        );
        BlockHitResult hitResult = new BlockHitResult(faceCenter, placeSide, supportPos, false);

        mc.player.swingHand(Hand.MAIN_HAND);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);

        if (signItem.isHotbar() && !signItem.isOffhand()) {
            InvUtils.swapBack();
        }

        state = State.WaitingForPlace;
        tickTimer = 0;
    }

    private static class PlaceSpot {
        final BlockPos supportPos, signPos;
        final Direction side;
        PlaceSpot(BlockPos supportPos, BlockPos signPos, Direction side) {
            this.supportPos = supportPos;
            this.signPos = signPos;
            this.side = side;
        }
    }

    /** Find a block face we can place a sign on (anywhere in front of player). */
    private PlaceSpot findPlaceSpotNearPlayer() {
        Vec3d start = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVec(1.0f);
        Vec3d end = start.add(look.multiply(5.0));
        RaycastContext ctx = new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player);
        HitResult hit = mc.world.raycast(ctx);
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return null;
        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockPos supportPos = blockHit.getBlockPos();
        Direction face = blockHit.getSide();
        BlockPos signPos = supportPos.offset(face);
        if (!mc.world.getBlockState(signPos).isAir()) return null;
        if (!mc.world.getBlockState(supportPos).isSolidBlock(mc.world, supportPos)) return null;
        return new PlaceSpot(supportPos, signPos, face);
    }

    private static boolean isSignItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.getItem() == Items.OAK_SIGN ||
            stack.getItem() == Items.SPRUCE_SIGN ||
            stack.getItem() == Items.BIRCH_SIGN ||
            stack.getItem() == Items.JUNGLE_SIGN ||
            stack.getItem() == Items.ACACIA_SIGN ||
            stack.getItem() == Items.DARK_OAK_SIGN ||
            stack.getItem() == Items.MANGROVE_SIGN ||
            stack.getItem() == Items.CHERRY_SIGN ||
            stack.getItem() == Items.BAMBOO_SIGN ||
            stack.getItem() == Items.CRIMSON_SIGN ||
            stack.getItem() == Items.WARPED_SIGN;
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

    /** Find the dropped sign ItemEntity nearest to the block we broke (walk to THIS, not where the sign was broken). */
    private ItemEntity findDroppedSignEntity(BlockPos nearPos) {
        if (mc.world == null || mc.player == null) return null;
        Vec3d center = Vec3d.ofCenter(nearPos);
        Box box = new Box(nearPos).expand(12);
        ItemEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : mc.world.getEntitiesByType(EntityType.ITEM, box, entity -> true)) {
            if (!(e instanceof ItemEntity itemEntity)) continue;
            if (!isSignItem(itemEntity.getStack())) continue;
            double d = e.getPos().squaredDistanceTo(center);
            if (d < bestDist) {
                bestDist = d;
                best = itemEntity;
            }
        }
        return best;
    }

    private BlockPos findSupportBlock(BlockPos signPos) {
        // Wall sign: support is the block the sign was attached to (we stored placeDirection when we broke it)
        if (placeDirection != null && placeDirection.getAxis().isHorizontal()) {
            BlockPos wallSupport = signPos.offset(placeDirection.getOpposite());
            BlockState s = mc.world.getBlockState(wallSupport);
            if (!s.isAir() && s.isSolidBlock(mc.world, wallSupport)) {
                return wallSupport;
            }
        }

        // Standing sign or fallback: block below
        BlockPos below = signPos.down();
        if (mc.world.getBlockState(below).isSolidBlock(mc.world, below)) {
            return below;
        }

        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos side = signPos.offset(dir);
            if (mc.world.getBlockState(side).isSolidBlock(mc.world, side)) {
                return side;
            }
        }

        return null;
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
        BlockPos posToUpdate = placePos != null ? placePos : currentTarget;

        if (posToUpdate != null && isSign(mc.world.getBlockState(posToUpdate))) {
            if (signTextSendsLeft <= 0) {
                signTextSendsLeft = 6;
                signTextTargetPos = posToUpdate;
            }
        }

        if (signTextTargetPos != null && signTextSendsLeft > 0) {
            sendSignTextPacket(signTextTargetPos);
            signTextSendsLeft--;
            if (mc.currentScreen instanceof SignEditScreen) mc.currentScreen.close();
            if (signTextSendsLeft == 0) {
                finishAfterPlace(signTextTargetPos);
                return;
            }
        }

        if (mc.currentScreen instanceof SignEditScreen && posToUpdate != null) {
            sendSignTextPacket(posToUpdate);
            mc.currentScreen.close();
            finishAfterPlace(posToUpdate);
            return;
        }

        if (tickTimer > 40) {
            if (posToUpdate != null) recentlyPlaced.put(posKey(posToUpdate), System.currentTimeMillis());
            if (placePos != null) signs.remove(placePos);
            if (currentTarget != null) signs.remove(currentTarget);
            state = State.Scanning;
            currentTarget = null;
            placePos = null;
            signTextSendsLeft = 0;
            signTextTargetPos = null;
        }
    }

    private void sendSignTextPacket(BlockPos pos) {
        try {
            mc.player.networkHandler.sendPacket(new UpdateSignC2SPacket(
                pos,
                true,
                line1.get(),
                line2.get(),
                line3.get(),
                line4.get()
            ));
        } catch (Throwable t) {
            info("Failed to set sign text: " + t.getMessage());
        }
    }

    private void finishAfterPlace(BlockPos justPlaced) {
        if (justPlaced != null) recentlyPlaced.put(posKey(justPlaced), System.currentTimeMillis());
        placementCooldownTicks = placementCooldown.get();
        placeRetryCount = 0;
        ticksWithSign = 0;
        signTextSendsLeft = 0;
        signTextTargetPos = null;
        state = State.Scanning;
        currentTarget = null;
        placePos = null;
        tickTimer = 0;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get()) return;

        // Cap boxes drawn to avoid FPS drops with huge sign lists
        int drawn = 0;
        for (BlockPos pos : signs) {
            if (drawn >= MAX_RENDER_BOXES) break;
            if (pos.equals(currentTarget)) continue;
            event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            drawn++;
        }

        if (currentTarget != null) {
            event.renderer.box(currentTarget, targetColor.get(), targetColor.get(), ShapeMode.Both, 0);
        }
    }
}
