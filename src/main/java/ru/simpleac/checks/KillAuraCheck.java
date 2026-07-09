package ru.simpleac.util;

import org.bukkit.Location;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class MathUtil {

    /**
     * Угол (в градусах) между направлением взгляда игрока (from) и вектором к точке target.
     */
    public static double angleBetween(Location from, Location target) {
        Vector look = from.getDirection().normalize();
        Vector toTarget = target.toVector().subtract(from.toVector()).normalize();
        double dot = look.dot(toTarget);
        dot = Math.max(-1.0, Math.min(1.0, dot));
        return Math.toDegrees(Math.acos(dot));
    }

    /**
     * Кратчайшее расстояние от точки eye до хитбокса box (а не до центра/глаз цели -
     * это важно, т.к. игроки часто целятся в ноги/тело, а не строго в глаза цели).
     */
    public static double distanceToBox(Location eye, BoundingBox box) {
        double clampedX = clamp(eye.getX(), box.getMinX(), box.getMaxX());
        double clampedY = clamp(eye.getY(), box.getMinY(), box.getMaxY());
        double clampedZ = clamp(eye.getZ(), box.getMinZ(), box.getMaxZ());
        double dx = eye.getX() - clampedX;
        double dy = eye.getY() - clampedY;
        double dz = eye.getZ() - clampedZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Направление горизонтального движения в градусах (0-360), как yaw.
     */
    public static double headingOf(double dx, double dz) {
        return Math.toDegrees(Math.atan2(-dx, dz)) + 180.0;
    }

    public static double normalizeAngleDiff(double diff) {
        diff = diff % 360.0;
        if (diff > 180.0) diff -= 360.0;
        if (diff < -180.0) diff += 360.0;
        return Math.abs(diff);
    }
}
