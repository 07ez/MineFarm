package Hez.PvP.Class;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public enum ColosseumClassType {

    GLADIATOR("검투사", "무기: 철검, 방어구: 철 세트"),
    ONE_PUNCH_MAN("원펀맨", "무기: 맨손, 방어구: 없음, 능력: 힘 125lv (허공 주먹 시 7 피해)"),
    FARMER("파머", "무기: 금괭이, 방어구: 사슬투구+구리갑옷(염색)"),
    FOOD_FIGHTER("푸드파이터", "무기: 맨손, 방어구: 철 세트"),
    TELEPORTER("텔레포터", "무기: 철검, 방어구: 사슬 세트"),
    MOLE("두더지", "무기: 철검, 방어구: 사슬 세트");

    private final String displayName;
    private final String description;

    ColosseumClassType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static ColosseumClassType fromDisplayName(String name) {
        if (name == null) return null;
        for (ColosseumClassType type : values()) {
            if (type.displayName.equalsIgnoreCase(name.trim())) {
                return type;
            }
        }
        return null;
    }

    /**
     * 콜로세움 전투 시작 시 해당 직업의 기본 키트(무기, 방어구) 지급
     */
    public void applyKit(Player player) {
        if (player == null) return;
        PlayerInventory inv = player.getInventory();
        inv.clear();

        switch (this) {
            case GLADIATOR -> {
                // 철검 + 철 세트
                inv.setItem(0, new ItemStack(Material.IRON_SWORD));
                inv.setHelmet(new ItemStack(Material.IRON_HELMET));
                inv.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                inv.setLeggings(new ItemStack(Material.IRON_LEGGINGS));
                inv.setBoots(new ItemStack(Material.IRON_BOOTS));
            }
            case ONE_PUNCH_MAN -> {
                // 맨손, 무방어구, 힘 125
                player.removePotionEffect(PotionEffectType.STRENGTH);
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, PotionEffect.INFINITE_DURATION, 124, false, false, true));
            }
            case FARMER -> {
                // 금괭이 + 사슬투구 + 구리색 가죽 갑옷
                inv.setItem(0, new ItemStack(Material.GOLDEN_HOE));
                inv.setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
                inv.setChestplate(createCopperArmor(Material.LEATHER_CHESTPLATE, "§6구리 흉갑"));
                inv.setLeggings(createCopperArmor(Material.LEATHER_LEGGINGS, "§6구리 레깅스"));
                inv.setBoots(createCopperArmor(Material.LEATHER_BOOTS, "§6구리 부츠"));
            }
            case FOOD_FIGHTER -> {
                // 맨손 + 철 세트
                inv.setHelmet(new ItemStack(Material.IRON_HELMET));
                inv.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                inv.setLeggings(new ItemStack(Material.IRON_LEGGINGS));
                inv.setBoots(new ItemStack(Material.IRON_BOOTS));
            }
            case TELEPORTER -> {
                // 철검 + 사슬 세트
                inv.setItem(0, new ItemStack(Material.IRON_SWORD));
                inv.setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
                inv.setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
                inv.setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
                inv.setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));
            }
            case MOLE -> {
                // 철검 + 사슬 세트
                inv.setItem(0, new ItemStack(Material.IRON_SWORD));
                inv.setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
                inv.setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
                inv.setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
                inv.setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));
            }
        }
    }

    private static ItemStack createCopperArmor(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof LeatherArmorMeta leatherMeta) {
            // 구리 오렌지빛 색상
            leatherMeta.setColor(Color.fromRGB(190, 106, 74));
            leatherMeta.setDisplayName(name);
            item.setItemMeta(leatherMeta);
        }
        return item;
    }
}
