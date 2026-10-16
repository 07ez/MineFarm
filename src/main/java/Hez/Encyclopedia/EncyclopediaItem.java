package Hez.Encyclopedia;

import org.bukkit.Material;

public record EncyclopediaItem(
        int index,
        String name,
        Material material
) {
}
