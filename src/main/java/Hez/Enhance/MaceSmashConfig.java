package Hez.Enhance;

public class MaceSmashConfig {
    private final int targetLevel;
    private final double successRate;          // 성공 확률
    private final double failureRate;          // 실패 확률 (실패 시 항상 단계 유지)
    private final double destroyRate;          // 파괴 확률
    private final double jumpVelocity;         // 도약 Y속도 (점프 높이)
    private final double cooldownSeconds;      // 발동 쿨타임 (초)
    private final double earthquakeRadius;     // 착지 시 지진 충격파 반경 (블록)
    private final double earthquakeDamage;     // 지진 충격파 피해량
    private final double airbornePower;        // 적을 공중에 띄우는 힘 (0 = 에어본 없음)
    private final long cost;                   // 강화 비용

    public MaceSmashConfig(int targetLevel, double successRate, double failureRate, double destroyRate,
                           double jumpVelocity, double cooldownSeconds, double earthquakeRadius,
                           double earthquakeDamage, double airbornePower, long cost) {
        this.targetLevel = targetLevel;
        this.successRate = successRate;
        this.failureRate = failureRate;
        this.destroyRate = destroyRate;
        this.jumpVelocity = jumpVelocity;
        this.cooldownSeconds = cooldownSeconds;
        this.earthquakeRadius = earthquakeRadius;
        this.earthquakeDamage = earthquakeDamage;
        this.airbornePower = airbornePower;
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

    public double getJumpVelocity() {
        return jumpVelocity;
    }

    public double getCooldownSeconds() {
        return cooldownSeconds;
    }

    public double getEarthquakeRadius() {
        return earthquakeRadius;
    }

    public double getEarthquakeDamage() {
        return earthquakeDamage;
    }

    public double getAirbornePower() {
        return airbornePower;
    }

    public boolean hasAirborne() {
        return airbornePower > 0;
    }

    public long getCost() {
        return cost;
    }
}
