package Hez.Crops;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

/**
 * 지원하는 농작물 종류 및 기본 설정 정보
 */
public enum CropType {
    WHEAT(Material.WHEAT, Material.WHEAT_SEEDS, 5 * 60 * 1000L, "밀"),
    CARROT(Material.CARROTS, Material.CARROT, 7 * 60 * 1000L, "당근"),
    POTATO(Material.POTATOES, Material.POTATO, 7 * 60 * 1000L, "감자"),
    BEETROOT(Material.BEETROOTS, Material.BEETROOT_SEEDS, 10 * 60 * 1000L, "비트"),
    NETHER_WART(Material.NETHER_WART, Material.NETHER_WART, 12 * 60 * 1000L, "네더 와트");

    private final Material blockMaterial;
    private final Material seedMaterial;
    private final long defaultGrowDurationMs;
    private final String displayName;

    private static final Map<Material, CropType> BY_BLOCK = new HashMap<>();
    private static final Map<Material, CropType> BY_SEED = new HashMap<>();

    static {
        for (CropType type : values()) {
            BY_BLOCK.put(type.blockMaterial, type);
            BY_SEED.put(type.seedMaterial, type);
        }
    }

    CropType(Material blockMaterial, Material seedMaterial, long defaultGrowDurationMs, String displayName) {
        this.blockMaterial = blockMaterial;
        this.seedMaterial = seedMaterial;
        this.defaultGrowDurationMs = defaultGrowDurationMs;
        this.displayName = displayName;
    }

    public Material getBlockMaterial() { return blockMaterial; }
    public Material getSeedMaterial() { return seedMaterial; }
    public long getDefaultGrowDurationMs() { return defaultGrowDurationMs; }
    public String getDisplayName() { return displayName; }

    public static CropType fromBlock(Material material) {
        return BY_BLOCK.get(material);
    }

    public static CropType fromSeed(Material material) {
        return BY_SEED.get(material);
    }
}
