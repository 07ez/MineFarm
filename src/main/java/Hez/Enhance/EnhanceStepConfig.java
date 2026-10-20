package Hez.Enhance;

public class EnhanceStepConfig {
    private final int targetLevel;
    private final double successRate;     // 성공 확률 (0 ~ 100)
    private final double failureRate;     // 실패 확률 (0 ~ 100, 실패 시 단계 유지)
    private final double destroyRate;     // 파괴 확률 (0 ~ 100)
    private final double bonusAttackSpeed;// 해당 강화 레벨 도달 시 적용되는 공격 속도 보너스
    private final long cost;              // 강화 필요 비용 (선택 사항)

    public EnhanceStepConfig(int targetLevel, double successRate, double failureRate, double destroyRate,
                             double bonusAttackSpeed, long cost) {
        this.targetLevel = targetLevel;
        this.successRate = successRate;
        this.failureRate = failureRate;
        this.destroyRate = destroyRate;
        this.bonusAttackSpeed = bonusAttackSpeed;
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

    public double getBonusAttackSpeed() {
        return bonusAttackSpeed;
    }

    public long getCost() {
        return cost;
    }

    /**
     * 기본 공격 속도(1.6) 기준 쿨타임 감소 퍼센트 계산
     */
    public double getCooldownReductionPercentage() {
        double baseSpeed = 1.6;
        double currentSpeed = baseSpeed + bonusAttackSpeed;
        double baseCooldown = 1.0 / baseSpeed;
        double newCooldown = 1.0 / currentSpeed;
        return ((baseCooldown - newCooldown) / baseCooldown) * 100.0;
    }
}
