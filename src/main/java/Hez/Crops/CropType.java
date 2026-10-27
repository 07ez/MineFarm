package Hez.Crops;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

/**
 * 지원하는 농작물 16종 및 기본 설정 정보
 */
public enum CropType {
    // 1. 감자: 1~3개 수확, 정식 1시간, 테스트 10초
    POTATO(Material.POTATOES, Material.POTATO, Material.POTATO, 1, 3, 1 * 3600 * 1000L, 10 * 1000L, "감자", HarvestType.BREAK_CROP),
    // 2. 당근: 1~3개 수확, 정식 1.5시간, 테스트 10초
    CARROT(Material.CARROTS, Material.CARROT, Material.CARROT, 1, 3, (long)(1.5 * 3600 * 1000L), 10 * 1000L, "당근", HarvestType.BREAK_CROP),
    // 3. 비트: 1개 수확, 정식 3시간, 테스트 10초
    BEETROOT(Material.BEETROOTS, Material.BEETROOT_SEEDS, Material.BEETROOT, 1, 1, 3 * 3600 * 1000L, 10 * 1000L, "비트", HarvestType.BREAK_CROP),
    // 4. 밀: 1개 수확, 정식 30시간, 테스트 10초
    WHEAT(Material.WHEAT, Material.WHEAT_SEEDS, Material.WHEAT, 1, 1, 30 * 3600 * 1000L, 10 * 1000L, "밀", HarvestType.BREAK_CROP),
    // 5. 횃불꽃: 1개 수확, 정식 10시간, 테스트 10초
    TORCHFLOWER(Material.TORCHFLOWER_CROP, Material.TORCHFLOWER_SEEDS, Material.TORCHFLOWER, 1, 1, 10 * 3600 * 1000L, 10 * 1000L, "횃불꽃", HarvestType.BREAK_CROP),
    // 6. 수박: 씨앗 자리에 수박 블록 생성, 파괴 시 수박 블록 1개 드롭 (일회성), 정식 7시간, 테스트 10초
    MELON(Material.MELON_STEM, Material.MELON_SEEDS, Material.MELON, 1, 1, 7 * 3600 * 1000L, 10 * 1000L, "수박", HarvestType.BLOCK_TRANSFORM),
    // 7. 호박: 씨앗 자리에 호박 블록 생성, 파괴 시 호박 블록 1개 드롭 (일회성), 정식 8시간, 테스트 10초
    PUMPKIN(Material.PUMPKIN_STEM, Material.PUMPKIN_SEEDS, Material.PUMPKIN, 1, 1, 8 * 3600 * 1000L, 10 * 1000L, "호박", HarvestType.BLOCK_TRANSFORM),
    // 8. 네더 와트: 3개 수확, 정식 1시간, 테스트 10초
    NETHER_WART(Material.NETHER_WART, Material.NETHER_WART, Material.NETHER_WART, 3, 3, 1 * 3600 * 1000L, 10 * 1000L, "네더 와트", HarvestType.BREAK_CROP),
    // 9. 후렴과: 단계당 정식 30분, 테스트 10초
    CHORUS_FRUIT(Material.CHORUS_FLOWER, Material.CHORUS_FLOWER, Material.CHORUS_FRUIT, 1, 2, 30 * 60 * 1000L, 10 * 1000L, "후렴과", HarvestType.CHORUS_TREE),
    // 10. 벌레잡이풀: 1개 수확, 정식 15시간, 테스트 10초
    PITCHER_PLANT(Material.PITCHER_CROP, Material.PITCHER_POD, Material.PITCHER_PLANT, 1, 1, 15 * 3600 * 1000L, 10 * 1000L, "벌레잡이풀", HarvestType.DOUBLE_PLANT),
    // 11. 사탕수수: 블록당 정식 30분, 테스트 10초, 최대 3단 타워형
    SUGAR_CANE(Material.SUGAR_CANE, Material.SUGAR_CANE, Material.SUGAR_CANE, 1, 1, 30 * 60 * 1000L, 10 * 1000L, "사탕수수", HarvestType.TOWER),
    // 12. 선인장: 블록당 정식 30분, 테스트 10초, 최대 3단 타워형
    CACTUS(Material.CACTUS, Material.CACTUS, Material.CACTUS, 1, 1, 30 * 60 * 1000L, 10 * 1000L, "선인장", HarvestType.TOWER),
    // 13. 켈프: 블록당 정식 10분, 테스트 10초, 최대 15단 타워형
    KELP(Material.KELP, Material.KELP, Material.KELP, 1, 1, 10 * 60 * 1000L, 10 * 1000L, "켈프", HarvestType.TOWER),
    // 14. 달콤한 열매: 파괴 시 열매 5개 드롭 후 덤불 소멸 (우클릭 수확 불가), 정식 5시간, 테스트 10초
    SWEET_BERRIES(Material.SWEET_BERRY_BUSH, Material.SWEET_BERRIES, Material.SWEET_BERRIES, 5, 5, 5 * 3600 * 1000L, 10 * 1000L, "달콤한 열매", HarvestType.BREAK_CROP),
    // 15. 빛나는 열매: 우클릭 수확(열매 1개) + 줄기 유지, 정식 3시간, 테스트 10초
    GLOW_BERRIES(Material.CAVE_VINES, Material.GLOW_BERRIES, Material.GLOW_BERRIES, 1, 1, 3 * 3600 * 1000L, 10 * 1000L, "빛나는 열매", HarvestType.VINE_RIGHT_CLICK),
    // 16. 코코아콩: 파괴 시 3개 드롭, 정식 1시간, 테스트 10초
    COCOA(Material.COCOA, Material.COCOA_BEANS, Material.COCOA_BEANS, 3, 3, 1 * 3600 * 1000L, 10 * 1000L, "코코아콩", HarvestType.BREAK_CROP);

    public enum HarvestType {
        BREAK_CROP,         // 일반 블록 파괴형 작물 (감자, 당근, 비트, 밀, 횃불꽃, 네더와트, 달콤한열매, 코코아)
        BLOCK_TRANSFORM,    // 완숙 시 수박/호박 블록으로 변환되는 일회성 작물
        DOUBLE_PLANT,       // 완숙 시 2칸 크기가 되는 작물 (벌레잡이풀)
        TOWER,              // 위로 최대 3단까지 자라며 밑동 보존 시 지속 수확 가능한 타워형 (사탕수수, 선인장, 켈프)
        CHORUS_TREE,        // 엔드스톤 위 후렴화 가지치기형 (후렴과)
        VINE_RIGHT_CLICK    // 천장 동굴 덩굴, 우클릭 시 열매 수확 및 덩굴 보존 (빛나는 열매)
    }

    // 테스트 모드 플래그: true이면 전 작물 테스트 시간(10초) 적용
    public static final boolean TEST_MODE = true;

    private final Material blockMaterial;
    private final Material seedMaterial;
    private final Material dropMaterial;
    private final int minDrop;
    private final int maxDrop;
    private final long officialGrowDurationMs;
    private final long testGrowDurationMs;
    private final String displayName;
    private final HarvestType harvestType;

    private static final Map<Material, CropType> BY_BLOCK = new HashMap<>();
    private static final Map<Material, CropType> BY_SEED = new HashMap<>();

    static {
        for (CropType type : values()) {
            BY_BLOCK.put(type.blockMaterial, type);
            BY_SEED.put(type.seedMaterial, type);
        }
        // 보조 블록 매핑
        BY_BLOCK.put(Material.MELON, MELON);
        BY_BLOCK.put(Material.ATTACHED_MELON_STEM, MELON);
        BY_BLOCK.put(Material.PUMPKIN, PUMPKIN);
        BY_BLOCK.put(Material.ATTACHED_PUMPKIN_STEM, PUMPKIN);
        BY_BLOCK.put(Material.CAVE_VINES_PLANT, GLOW_BERRIES);
        BY_BLOCK.put(Material.KELP_PLANT, KELP);
        BY_BLOCK.put(Material.CHORUS_PLANT, CHORUS_FRUIT);
        BY_BLOCK.put(Material.PITCHER_PLANT, PITCHER_PLANT);
    }

    CropType(Material blockMaterial, Material seedMaterial, Material dropMaterial,
             int minDrop, int maxDrop,
             long officialGrowDurationMs, long testGrowDurationMs,
             String displayName, HarvestType harvestType) {
        this.blockMaterial = blockMaterial;
        this.seedMaterial = seedMaterial;
        this.dropMaterial = dropMaterial;
        this.minDrop = minDrop;
        this.maxDrop = maxDrop;
        this.officialGrowDurationMs = officialGrowDurationMs;
        this.testGrowDurationMs = testGrowDurationMs;
        this.displayName = displayName;
        this.harvestType = harvestType;
    }

    public Material getBlockMaterial() { return blockMaterial; }
    public Material getSeedMaterial() { return seedMaterial; }
    public Material getDropMaterial() { return dropMaterial; }
    public int getMinDrop() { return minDrop; }
    public int getMaxDrop() { return maxDrop; }
    public long getOfficialGrowDurationMs() { return officialGrowDurationMs; }
    public long getTestGrowDurationMs() { return testGrowDurationMs; }

    public long getDefaultGrowDurationMs() {
        return TEST_MODE ? testGrowDurationMs : officialGrowDurationMs;
    }

    public String getDisplayName() { return displayName; }
    public HarvestType getHarvestType() { return harvestType; }

    public static CropType fromBlock(Material material) {
        return BY_BLOCK.get(material);
    }

    public static CropType fromSeed(Material material) {
        return BY_SEED.get(material);
    }
}
