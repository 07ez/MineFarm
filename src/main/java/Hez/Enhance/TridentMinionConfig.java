package Hez.Enhance;

public class TridentMinionConfig {
    private final int targetLevel;
    private final double successRate;          // 성공 확률
    private final double failureRate;          // 실패 확률 (실패 시 항상 단계 유지)
    private final double destroyRate;          // 파괴 확률
    private final int minionCount;             // 소환되는 삼지창 미니언 수 (1 ~ 10개)
    private final double cooldownSeconds;      // 미니언 일제 사출 쿨타임 (초)
    private final double damagePerMinion;      // 미니언 1기당 데미지
    private final long cost;                   // 강화 비용

    public TridentMinionConfig(int targetLevel, double successRate, double failureRate, double destroyRate,
                               int minionCount, double cooldownSeconds, double damagePerMinion, long cost) {
        this.targetLevel = targetLevel;
        this.successRate = successRate;
        this.failureRate = failureRate;
        this.destroyRate = destroyRate;
        this.minionCount = minionCount;
        this.cooldownSeconds = cooldownSeconds;
        this.damagePerMinion = damagePerMinion;
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

    public int getMinionCount() {
        return minionCount;
    }

    public double getCooldownSeconds() {
        return cooldownSeconds;
    }

    public double getDamagePerMinion() {
        return damagePerMinion;
    }

    public long getCost() {
        return cost;
    }
}
