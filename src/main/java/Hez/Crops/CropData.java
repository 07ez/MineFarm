package Hez.Crops;

import org.bukkit.Material;

/**
 * 특정 위치에 심겨진 개별 작물의 성장 데이터
 */
public class CropData {
    private final BlockPos pos;
    private final Material cropMaterial;
    private final long plantedAt;         // 심은 시각 (ms)
    private final long growDurationMs;     // 완전히 자라는 데 걸리는 기본 시간 (ms)
    private float boost;                   // 성장 가속 계수 (기본 1.0f)
    private boolean boneMealUsed;          // 뼛가루 10% 단축 사용 여부 (딱 1회만 가능)

    public CropData(BlockPos pos, Material cropMaterial, long plantedAt, long growDurationMs) {
        this(pos, cropMaterial, plantedAt, growDurationMs, 1.0f, false);
    }

    public CropData(BlockPos pos, Material cropMaterial, long plantedAt, long growDurationMs, float boost) {
        this(pos, cropMaterial, plantedAt, growDurationMs, boost, false);
    }

    public CropData(BlockPos pos, Material cropMaterial, long plantedAt, long growDurationMs, float boost, boolean boneMealUsed) {
        this.pos = pos;
        this.cropMaterial = cropMaterial;
        this.plantedAt = plantedAt;
        this.growDurationMs = growDurationMs;
        this.boost = boost;
        this.boneMealUsed = boneMealUsed;
    }

    public BlockPos getPos() {
        return pos;
    }

    public Material getCropMaterial() {
        return cropMaterial;
    }

    public long getPlantedAt() {
        return plantedAt;
    }

    public long getGrowDurationMs() {
        return growDurationMs;
    }

    public float getBoost() {
        return boost;
    }

    public void setBoost(float boost) {
        this.boost = boost;
    }

    public boolean isBoneMealUsed() {
        return boneMealUsed;
    }

    public void setBoneMealUsed(boolean boneMealUsed) {
        this.boneMealUsed = boneMealUsed;
    }

    /**
     * 뼛가루 사용 (딱 1회만 가능):
     * 전체 자라는 시간의 10% (growDurationMs / 10)를 단축
     * @return 뼛가루가 성공적으로 적용되었으면 true, 이미 사용되었으면 false
     */
    public boolean applyBoneMeal() {
        if (boneMealUsed || isHarvestable()) {
            return false;
        }
        this.boneMealUsed = true;
        return true;
    }

    /**
     * 부스트 및 뼛가루 단축을 적용한 유효 성장 필요 시간 (ms)
     * - 뼛가루 사용 시 전체 자라는 시간의 10% (growDurationMs / 10) 차감
     */
    public long getEffectiveDurationMs() {
        long baseDuration = (long) (growDurationMs / Math.max(0.1f, boost));
        if (boneMealUsed) {
            long reduction = growDurationMs / 10L;
            return Math.max(0L, baseDuration - reduction);
        }
        return baseDuration;
    }

    /**
     * 남은 시간 (ms) 계산:
     * (심은시간 + 자라는시간) - 현재시간
     * 0 이하면 다 자란 상태
     */
    public long getRemainingTimeMillis() {
        long finishTime = plantedAt + getEffectiveDurationMs();
        long remaining = finishTime - System.currentTimeMillis();
        return Math.max(0L, remaining);
    }

    /**
     * 남은 시간이 0이 되었을 때 수확 가능 상태
     */
    public boolean isHarvestable() {
        return getRemainingTimeMillis() <= 0;
    }

    /**
     * 블록의 최대 나이(maxAge) 기준, 현재 경과 시간에 따른 나이 단계(Age) 계산
     * @param maxAge 해당 작물의 최대 나이 (예: 밀 7, 비트 3)
     * @return 0 ~ maxAge 사이의 현재 나이
     */
    public int calculateAge(int maxAge) {
        if (isHarvestable()) {
            return maxAge;
        }

        long effectiveDuration = getEffectiveDurationMs();
        if (effectiveDuration <= 0) {
            return maxAge;
        }

        long elapsed = System.currentTimeMillis() - plantedAt;
        if (elapsed <= 0) {
            return 0;
        }

        int calculatedAge = (int) ((elapsed * maxAge) / effectiveDuration);
        return Math.clamp(calculatedAge, 0, maxAge);
    }

    /**
     * 남은 시간을 00:00:00 (시:분:초) 형태로 반환하거나,
     * 다 자랐을 경우 "수확가능!" 반환
     */
    public String getFormattedRemainingTime() {
        if (isHarvestable()) {
            return "수확가능!";
        }
        long remainingSec = getRemainingTimeMillis() / 1000L;
        long hours = remainingSec / 3600L;
        long minutes = (remainingSec % 3600L) / 60L;
        long seconds = remainingSec % 60L;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
