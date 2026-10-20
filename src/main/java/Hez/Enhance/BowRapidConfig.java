package Hez.Enhance;

public class BowRapidConfig {
    private final int targetLevel;
    private final double successRate;          // 성공 확률
    private final double failureRate;          // 실패 확률 (실패 시 항상 단계 유지)
    private final double destroyRate;          // 파괴 확률
    private final double chargeBonus;          // 시위 차징 가속 배율 (0.15 = +15% ~ 1.0 = 100%)
    private final double projectileSpeedBonus; // 화살 비행 속도 가속 배율
    private final int arrowCount;              // 동시 발사 화살 수 (1 ~ 5발)
    private final int pierceLevel;             // 관통 엔티티 수 (10강 시 1회 관통 등)
    private final long cost;                   // 강화 비용

    public BowRapidConfig(int targetLevel, double successRate, double failureRate, double destroyRate,
                          double chargeBonus, double projectileSpeedBonus, int arrowCount,
                          int pierceLevel, long cost) {
        this.targetLevel = targetLevel;
        this.successRate = successRate;
        this.failureRate = failureRate;
        this.destroyRate = destroyRate;
        this.chargeBonus = chargeBonus;
        this.projectileSpeedBonus = projectileSpeedBonus;
        this.arrowCount = arrowCount;
        this.pierceLevel = pierceLevel;
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

    public double getChargeBonus() {
        return chargeBonus;
    }

    public int getChargePercent() {
        return (int) Math.round(chargeBonus * 100);
    }

    public double getProjectileSpeedBonus() {
        return projectileSpeedBonus;
    }

    public int getArrowCount() {
        return arrowCount;
    }

    public boolean isMultiShot() {
        return arrowCount > 1;
    }

    public int getPierceLevel() {
        return pierceLevel;
    }

    public long getCost() {
        return cost;
    }
}
