package Hez.Encyclopedia;

import org.bukkit.Material;

public record EncyclopediaItem(
        int index,
        String name,
        Material material,
        int requiredAmount
) {
    public EncyclopediaItem(int index, String name, Material material) {
        this(index, name, material, 10);
    }
}
