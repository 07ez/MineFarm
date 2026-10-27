package Hez.Casino.Slot;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public enum SlotSymbol {
    COAL("§8석탄", Material.COAL, 40, 2),
    IRON_INGOT("§f철괴", Material.IRON_INGOT, 25, 4),
    GOLD_INGOT("§e금괴", Material.GOLD_INGOT, 15, 8),
    EMERALD("§a에메랄드", Material.EMERALD, 10, 15),
    DIAMOND("§b다이아몬드", Material.DIAMOND, 7, 30),
    NETHERITE_INGOT("§5네더라이트", Material.NETHERITE_INGOT, 3, 100);

    private final String name;
    private final Material material;
    private final int weight;
    private final int multiplier;

    SlotSymbol(String name, Material material, int weight, int multiplier) {
        this.name = name;
        this.material = material;
        this.weight = weight;
        this.multiplier = multiplier;
    }

    public String getName() {
        return name;
    }

    public Material getMaterial() {
        return material;
    }

    public int getWeight() {
        return weight;
    }

    public int getMultiplier() {
        return multiplier;
    }

    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of("§7슬롯 심볼 (배당: §e" + multiplier + "배§7)"));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 가중치(총 100) 기반 무작위 심볼 추출
     */
    public static SlotSymbol getRandom() {
        int rand = ThreadLocalRandom.current().nextInt(100); // 0 ~ 99
        int cumulative = 0;
        for (SlotSymbol s : values()) {
            cumulative += s.weight;
            if (rand < cumulative) {
                return s;
            }
        }
        return COAL;
    }

    /**
     * 연출용 순수 랜덤 심볼 (균등 확률)
     */
    public static SlotSymbol getRandomVisual() {
        SlotSymbol[] values = values();
        return values[ThreadLocalRandom.current().nextInt(values.length)];
    }
}
