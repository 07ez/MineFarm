package Hez.CustomItem.Fish;

import org.bukkit.block.Biome;

public class BiomeChecker {
    public enum CustomBiomeCategory {
        OCEAN,     // 바다
        CAVE,      // 동굴
        RIVER,     // 강
        DESERT,    // 사막
        LAKE,      // 호수
        ICE,       // 얼음
        DEADWATER, // 썩은물 (쓰레기만 나옴)
        OTHER      // 기타
    }

    public static CustomBiomeCategory getCategory(Biome biome) {
        String biomeName = biome.getKey().getKey().toUpperCase();

        // Ocean
        if (biomeName.contains("OCEAN") || biomeName.contains("BEACH") || biomeName.contains("SHORE") || biomeName.contains("MUSHROOM")) {
            return CustomBiomeCategory.OCEAN;
        }

        // Ice
        if (biomeName.contains("ICE") || biomeName.contains("SNOWY") || biomeName.contains("FROZEN")) {
            return CustomBiomeCategory.ICE;
        }

        // Cave
        if (biomeName.contains("CAVE") || biomeName.contains("DEEP_DARK")) {
            return CustomBiomeCategory.CAVE;
        }

        // River
        if (biomeName.contains("RIVER") || biomeName.contains("HILLS") || biomeName.contains("JUNGLE")) {
            return CustomBiomeCategory.RIVER;
        }

        // Desert
        if (biomeName.contains("DESERT") || biomeName.contains("SAVANNA") || biomeName.contains("BADLANDS")) {
            return CustomBiomeCategory.DESERT;
        }

        // Lake
        if (biomeName.contains("PLAINS") || biomeName.contains("GROVE") || biomeName.contains("FOREST") || biomeName.contains("GARDEN") || biomeName.contains("TAIGA") || biomeName.contains("SWAMP")) {
            return CustomBiomeCategory.LAKE;
        }

        // Deadwater
        if (biomeName.contains("MEADOW") || biomeName.contains("JAGGED") || biomeName.contains("STONEY")){
            return CustomBiomeCategory.DEADWATER;
        }

        return CustomBiomeCategory.OTHER;
    }
}
