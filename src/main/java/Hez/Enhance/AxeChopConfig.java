package Hez.Enhance;

public class AxeChopConfig {
    private final int targetLevel;
    private final double successRate;      // 성공 확률
    private final double failureRate;      // 실패 확률 (실패 시 항상 단계 유지)
    private final double destroyRate;      // 파괴 확률
    private final int maxBlocks;           // 한 번에 연쇄 벌목 가능한 최대 원목 개수
    private final double cooldownSeconds;  // 찹트리 발동 쿨타임 (초)
    private final boolean breakLeaves;     // 나뭇잎 함께 파괴 여부
    private final double saveDurabilityRate; // 내구도 소모 무시 확률 (0 ~ 100%)
    private final long cost;

    public AxeChopConfig(int targetLevel, double successRate, double failureRate, double destroyRate,
                         int maxBlocks, double cooldownSeconds, boolean breakLeaves,
                         double saveDurabilityRate, long cost) {
        this.targetLevel = targetLevel;
        this.successRate = successRate;
        this.failureRate = failureRate;
        this.destroyRate = destroyRate;
        this.maxBlocks = maxBlocks;
        this.cooldownSeconds = cooldownSeconds;
        this.breakLeaves = breakLeaves;
        this.saveDurabilityRate = saveDurabilityRate;
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

    public int getMaxBlocks() {
        return maxBlocks;
    }

    public double getCooldownSeconds() {
        return cooldownSeconds;
    }

    public boolean isBreakLeaves() {
        return breakLeaves;
    }

    public double getSaveDurabilityRate() {
        return saveDurabilityRate;
    }

    public long getCost() {
        return cost;
    }
}
