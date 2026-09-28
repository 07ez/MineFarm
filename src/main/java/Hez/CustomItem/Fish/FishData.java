package Hez.CustomItem.Fish; // 패키지명 입력

import Hez.CustomItem.CreateItem;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.inventory.ItemStack;
import java.util.*;

public record FishData(
        Material base,
        String modelData,
        String displayName,
        Set<BiomeChecker.CustomBiomeCategory> category,
        WeatherCondition weather,
        int startTime,
        int endTime,
        double chance
) {
    public enum WeatherCondition{ ANY, CLEAR, RAIN}
    public ItemStack createItemStack() {
        return CreateItem.create(this.base, this.modelData, this.displayName);
    }

    public FishData(Material base, String modelData, String displayName,
                    int startTime, int endTime, double chance, WeatherCondition weather, BiomeChecker.CustomBiomeCategory... categories) {
        this(base, modelData, displayName, Set.of(categories), weather, startTime, endTime, chance);
    }

    public double getChance() { return chance; }

    public boolean isCatchableTime(long worldTime){
        if (startTime == endTime) return true;
        if (startTime <= endTime) return worldTime >= startTime && worldTime <= endTime;
        else return worldTime >= startTime || worldTime <= endTime;
    }

    public boolean isCatchableWeather(World world) {
        // 아무날씨 상관없음
        if(weather == WeatherCondition.ANY) return true;
        // 비
        if(weather == WeatherCondition.RAIN) return world.hasStorm();
        // 맑음
        if(weather == WeatherCondition.CLEAR) return !world.hasStorm();

        return false;
    }

    public boolean isCatchaleBiome(Biome biome) {
        return category.contains(BiomeChecker.getCategory(biome)) || category == null;
    }
}