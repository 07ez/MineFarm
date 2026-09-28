package Hez.CustomItem;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

import java.util.List;

public class CreateItem {
    public static ItemStack create(Material material, String customModelData, String displayName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (displayName != null && !displayName.isEmpty()) {
                meta.setDisplayName("§r" + displayName);
            }

            if (customModelData != null && !customModelData.isEmpty()) {
                CustomModelDataComponent component = meta.getCustomModelDataComponent();
                component.setStrings(List.of(customModelData));
                meta.setCustomModelDataComponent(component);
            }

            item.setItemMeta(meta);
        }
        return item;
    }
}
