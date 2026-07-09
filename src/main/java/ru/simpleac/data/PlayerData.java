package ru.simpleac.data;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Хранит всё состояние, необходимое проверкам, для одного игрока.
 */
public class PlayerData {

    // Очки нарушений по каждому чеку
    public final Map<String, Double> violations = new HashMap<>();

    // Общие
    public long lastMoveTime = System.currentTimeMillis();
    public double lastX, lastY, lastZ;
    public float lastYaw, lastPitch;
    public int airTicks = 0;
    public boolean wasOnGround = true;

    // KillAura
    public long lastSwingTime = 0L;
    public final Deque<Long> recentAttackTimes = new ArrayDeque<>();
    public String lastAttackedTargetId = null;
    public long lastAttackTime = 0L;

    // Baritone / straight-line
    public int straightLineTicks = 0;
    public int perfectRotationTicks = 0;
    public Double lastMoveHeading = null; // направление движения в градусах
    public Float lastLookYawAtMove = null;

    // Speed (скользящее окно)
    public double speedWindowDistance = 0.0;
    public long speedWindowStartMs = System.currentTimeMillis();
    public int speedViolationStreak = 0;

    // Scaffold
    public final Deque<Long> recentPlacements = new ArrayDeque<>();

    // AutoClicker
    public final Deque<Long> recentClicks = new ArrayDeque<>();

    public double getVl(String check) {
        return violations.getOrDefault(check, 0.0);
    }

    public void setVl(String check, double value) {
        violations.put(check, value);
    }

    public void addVl(String check, double amount) {
        violations.merge(check, amount, Double::sum);
    }
}
