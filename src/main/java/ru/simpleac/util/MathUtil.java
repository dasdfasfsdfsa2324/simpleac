package ru.simpleac.util;

import org.bukkit.Location;
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
