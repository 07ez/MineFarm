package Hez.Enhance;

public class SpearBleedConfig {
    private final int targetLevel;
    private final double successRate;          // 성공 확률
    private final double failureRate;          // 실패 확률 (실패 시 항상 단계 유지)
    private final double destroyRate;          // 파괴 확률
    private final double armorPenetrationRate; // 방어력 관통율 (0.10 = 10% ~ 1.00 = 100% 완전 관통)
    private final double bleedChance;          // 출혈 발생 확률 (0 ~ 100%)
    private final double bleedDurationSeconds; // 출혈 지속 시간 (초)
    private final double bleedDamagePerTick;   // 0.5초당 출혈 피해
    private final int slownessAmplifier;       // 둔화 단계 (-1: 둔화 없음, 0: 둔화 I, 1: 둔화 II, 2: 둔화 III)
    private final long cost;                   // 강화 비용

    public SpearBleedConfig(int targetLevel, double successRate, double failureRate, double destroyRate,
                            double armorPenetrationRate, double bleedChance, double bleedDurationSeconds,
                            double bleedDamagePerTick, int slownessAmplifier, long cost) {
        this.targetLevel = targetLevel;
        this.successRate = successRate;
        this.failureRate = failureRate;
        this.destroyRate = destroyRate;
        this.armorPenetrationRate = armorPenetrationRate;
        this.bleedChance = bleedChance;
        this.bleedDurationSeconds = bleedDurationSeconds;
        this.bleedDamagePerTick = bleedDamagePerTick;
        this.slownessAmplifier = slownessAmplifier;
        this.cost = cost;
    }

    public int getTargetLevel() {
        return targetLevel;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public double getFailureRate() {
        return failureRate;
    }

    public double getDestroyRate() {
        return destroyRate;
    }

    public double getArmorPenetrationRate() {
        return armorPenetrationRate;
    }

    public int getArmorPenetrationPercent() {
        return (int) Math.round(armorPenetrationRate * 100);
    }

    public double getBleedChance() {
        return bleedChance;
    }

    public double getBleedDurationSeconds() {
        return bleedDurationSeconds;
    }

    public double getBleedDamagePerTick() {
        return bleedDamagePerTick;
    }

    public int getSlownessAmplifier() {
        return slownessAmplifier;
    }

    public long getCost() {
        return cost;
    }
}
