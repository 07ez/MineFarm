package Hez.Enhance;

public class PickaxeMiningConfig {
    private final int targetLevel;
    private final double successRate;          // 성공 확률
    private final double failureRate;          // 실패 확률 (실패 시 항상 단계 유지)
    private final double destroyRate;          // 파괴 확률
    private final double speedBonus;           // 채굴 속도 증가 배율 (0.20 = +20%)
    private final AreaMiningType areaType;     // 채굴 범위 타입
    private final boolean autoPickup;          // 드롭 아이템 인벤토리 자동 수거
    private final long cost;                   // 강화 비용

    public PickaxeMiningConfig(int targetLevel, double successRate, double failureRate, double destroyRate,
                               double speedBonus, AreaMiningType areaType, boolean autoPickup, long cost) {
        this.targetLevel = targetLevel;
        this.successRate = successRate;
        this.failureRate = failureRate;
        this.destroyRate = destroyRate;
        this.speedBonus = speedBonus;
        this.areaType = areaType;
        this.autoPickup = autoPickup;
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

    public double getSpeedBonus() {
        return speedBonus;
    }

    public int getSpeedPercent() {
        return (int) Math.round(speedBonus * 100);
    }

    public AreaMiningType getAreaType() {
        return areaType;
    }

    public boolean isAutoPickup() {
        return autoPickup;
    }

    public long getCost() {
        return cost;
    }
}
