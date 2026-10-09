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

    public CropData(BlockPos pos, Material cropMaterial, long plantedAt, long growDurationMs) {
        this(pos, cropMaterial, plantedAt, growDurationMs, 1.0f);
    }

    public CropData(BlockPos pos, Material cropMaterial, long plantedAt, long growDurationMs, float boost) {
        this.pos = pos;
        this.cropMaterial = cropMaterial;
        this.plantedAt = plantedAt;
        this.growDurationMs = growDurationMs;
        this.boost = boost;
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

    /**
     * 부스트를 적용한 유효 성장 필요 시간 (ms)
     */
    public long getEffectiveDurationMs() {
        return (long) (growDurationMs / Math.max(0.1f, boost));
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
}
