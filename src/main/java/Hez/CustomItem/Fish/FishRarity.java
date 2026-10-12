package Hez.CustomItem.Fish;

/**
 * 낚시 물고기 희귀도 등급
 * (플레이어에게는 노출되지 않으며, 내부 난이도 / 시간 / 클릭수 / 가중치 결정용)
 */
public enum FishRarity {
    COMMON("일반", 2.5, 3, 71.0),
    RARE("희귀", 3.5, 5, 25.0),
    EPIC("영웅", 3.5, 6, 3.0),
    LEGEND("전설", 4.5, 10, 1.0);

    private final String koreanName;
    private final double timeLimitSeconds;
    private final int targetHits;
    private final double weight;

    FishRarity(String koreanName, double timeLimitSeconds, int targetHits, double weight) {
        this.koreanName = koreanName;
        this.timeLimitSeconds = timeLimitSeconds;
        this.targetHits = targetHits;
        this.weight = weight;
    }

    public String getKoreanName() { return koreanName; }
    public double getTimeLimitSeconds() { return timeLimitSeconds; }
    public int getTargetHits() { return targetHits; }
    public double getWeight() { return weight; }
}
