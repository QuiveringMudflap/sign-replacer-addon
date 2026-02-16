package com.signreplacer.addon.baritone;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Optional Baritone integration via reflection. Requires Baritone mod in mods folder.
 * Used for pathfinding and mining so the player moves and breaks blocks like on 2b2t.
 */
public final class BaritoneHelper {
    private static Boolean available = null;
    private static Object baritone = null;
    private static Object pathingBehavior = null;
    private static Object customGoalProcess = null;
    private static Object mineProcess = null;

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
            try {
                mineProcess = baritone.getClass().getMethod("getMineProcess").invoke(baritone);
            } catch (Throwable ignored) {}
            available = pathingBehavior != null && customGoalProcess != null;
        } catch (Throwable e) {
            available = false;
        }
        return available;
    }

    /** Start pathing to a block (get adjacent to it). Sets Baritone goal so the goal indicator shows. Cancels any current path first. */
    public static boolean pathTo(BlockPos pos) {
        if (!isAvailable() || customGoalProcess == null) return false;
        cancelPath();
        Object goal = null;
        try {
            Class<?> goalGetToBlock = Class.forName("baritone.api.pathing.goals.GoalGetToBlock");
            goal = goalGetToBlock.getConstructor(BlockPos.class).newInstance(pos);
        } catch (Throwable e) {
            try {
                Class<?> goalBlock = Class.forName("baritone.api.pathing.goals.GoalBlock");
                goal = goalBlock.getConstructor(int.class, int.class, int.class).newInstance(pos.getX(), pos.getY(), pos.getZ());
            } catch (Throwable e2) {
                return false;
            }
        }
        if (goal == null) return false;
        try {
            Class<?> goalClass = Class.forName("baritone.api.pathing.goals.Goal");
            java.lang.reflect.Method setGoalAndPath = customGoalProcess.getClass().getMethod("setGoalAndPath", goalClass);
            setGoalAndPath.invoke(customGoalProcess, goal);
            return true;
        } catch (Throwable e) {
            for (java.lang.reflect.Method m : customGoalProcess.getClass().getMethods()) {
                if (m.getParameterCount() == 1 && (m.getName().equals("setGoalAndPath") || m.getName().equals("setGoal"))) {
                    try {
                        m.invoke(customGoalProcess, goal);
                        return true;
                    } catch (Throwable ignored) {}
                }
            }
            return false;
        }
    }

    /** Start pathing to a position (block pos from vec). */
    public static boolean pathTo(Vec3d pos) {
        return pathTo(BlockPos.ofFloored(pos));
    }

    /** Cancel current path. Tries multiple method names for Meteor vs standalone Baritone. */
    public static void cancelPath() {
        if (!isAvailable() || pathingBehavior == null) return;
        Class<?> c = pathingBehavior.getClass();
        for (String methodName : new String[]{"cancelEverything", "forceCancel", "cancel"}) {
            try {
                c.getMethod(methodName).invoke(pathingBehavior);
                return;
            } catch (NoSuchMethodException e) {
                // try next name
            } catch (Throwable e) {
                return;
            }
        }
        // Fallback: find any no-arg method whose name contains "cancel"
        try {
            for (java.lang.reflect.Method m : c.getMethods()) {
                if (m.getParameterCount() == 0 && m.getName().toLowerCase().contains("cancel")) {
                    m.invoke(pathingBehavior);
                    return;
                }
            }
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

    /** Start mining the given block type (Baritone will path to and break it). Call cancelPath() first if you were pathing. */
    public static boolean mineBlock(Block block) {
        if (!isAvailable() || block == null || mineProcess == null) return false;
        cancelPath();
        try {
            mineProcess.getClass().getMethod("mine", Block[].class).invoke(mineProcess, (Object) new Block[]{block});
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    /** Cancel current mining task. */
    public static void cancelMining() {
        if (!isAvailable() || mineProcess == null) return;
        try {
            mineProcess.getClass().getMethod("cancel").invoke(mineProcess);
        } catch (Throwable ignored) {}
    }
}
