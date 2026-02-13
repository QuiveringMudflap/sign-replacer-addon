package com.signreplacer.addon.baritone;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Optional Baritone integration via reflection. Requires Baritone mod in mods folder.
 * Used for pathfinding so the player can jump, climb, and move normally on 2b2t.
 */
public final class BaritoneHelper {
    private static Boolean available = null;
    private static Object baritone = null;
    private static Object pathingBehavior = null;
    private static Object customGoalProcess = null;

    public static boolean isAvailable() {
        if (available != null) return available;
        try {
            Class<?> api = Class.forName("baritone.api.BaritoneAPI");
            Object provider = api.getMethod("getProvider").invoke(null);
            baritone = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
            if (baritone == null) {
                available = false;
                return false;
            }
            pathingBehavior = baritone.getClass().getMethod("getPathingBehavior").invoke(baritone);
            customGoalProcess = baritone.getClass().getMethod("getCustomGoalProcess").invoke(baritone);
            available = pathingBehavior != null && customGoalProcess != null;
        } catch (Throwable e) {
            available = false;
        }
        return available;
    }

    /** Start pathing to a block (get adjacent to it). */
    public static boolean pathTo(BlockPos pos) {
        if (!isAvailable() || customGoalProcess == null) return false;
        try {
            Class<?> goalClass = Class.forName("baritone.api.pathing.goals.GoalGetToBlock");
            Object goal = goalClass.getConstructor(BlockPos.class).newInstance(pos);
            customGoalProcess.getClass().getMethod("setGoalAndPath", Class.forName("baritone.api.pathing.goals.Goal")).invoke(customGoalProcess, goal);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    /** Start pathing to a position (block pos from vec). */
    public static boolean pathTo(Vec3d pos) {
        return pathTo(BlockPos.ofFloored(pos));
    }

    /** Cancel current path. */
    public static void cancelPath() {
        if (!isAvailable() || pathingBehavior == null) return;
        try {
            pathingBehavior.getClass().getMethod("cancelEverything").invoke(pathingBehavior);
        } catch (Throwable ignored) {}
    }

    /** True if we are currently pathing. */
    public static boolean isPathing() {
        if (!isAvailable() || pathingBehavior == null) return false;
        try {
            Object b = pathingBehavior.getClass().getMethod("isPathing").invoke(pathingBehavior);
            return Boolean.TRUE.equals(b);
        } catch (Throwable e) {
            return false;
        }
    }

    /** True if player block pos is at goal (within 1 block of target). */
    public static boolean isAtGoal(BlockPos target, BlockPos playerPos) {
        int dx = Math.abs(playerPos.getX() - target.getX());
        int dy = Math.abs(playerPos.getY() - target.getY());
        int dz = Math.abs(playerPos.getZ() - target.getZ());
        return dx <= 1 && dy <= 1 && dz <= 1;
    }

    /** True if we're close enough to the block to interact (adjacent). */
    public static boolean isAdjacentTo(BlockPos target, BlockPos playerPos) {
        int dx = Math.abs(playerPos.getX() - target.getX());
        int dy = Math.abs(playerPos.getY() - target.getY());
        int dz = Math.abs(playerPos.getZ() - target.getZ());
        return dx <= 1 && dy <= 1 && dz <= 1 && (dx + dy + dz) <= 2;
    }
}
