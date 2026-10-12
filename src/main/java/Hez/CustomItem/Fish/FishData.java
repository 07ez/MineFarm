package Hez.CustomItem.Fish;

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
        FishRarity rarity,
        Set<BiomeChecker.CustomBiomeCategory> category,
        WeatherCondition weather,
        int startTime,
        int endTime,
        double chance
) {
    public enum WeatherCondition { ANY, CLEAR, RAIN }

    public ItemStack createItemStack() {
        return CreateItem.create(this.base, this.modelData, this.displayName);
    }

    public FishData(Material base, String modelData, String displayName, FishRarity rarity,
                    int startTime, int endTime, double chance, WeatherCondition weather, BiomeChecker.CustomBiomeCategory... categories) {
        this(base, modelData, displayName, rarity, Set.of(categories), weather, startTime, endTime, chance);
    }

    public double getChance() { return chance; }

    public boolean isCatchableTime(long worldTime) {
        if (startTime == endTime) return true;
        if (startTime <= endTime) return worldTime >= startTime && worldTime <= endTime;
        else return worldTime >= startTime || worldTime <= endTime;
    }

    public boolean isCatchableWeather(World world) {
        if (weather == WeatherCondition.ANY) return true;
        if (weather == WeatherCondition.RAIN) return world.hasStorm();
        if (weather == WeatherCondition.CLEAR) return !world.hasStorm();
        return false;
    }

    public boolean isCatchaleBiome(Biome biome) {
        return category == null || category.contains(BiomeChecker.getCategory(biome));
    }
}