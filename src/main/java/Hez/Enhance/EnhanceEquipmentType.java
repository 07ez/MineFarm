package Hez.Enhance;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public enum EnhanceEquipmentType {
    SWORD("검", "휘두르기 쿨타임 감소"),
    AXE("도끼", "찹트리 (연쇄 벌목)"),
    PICKAXE("곡괭이", "채굴 가속 & 범위 채굴"),
    SPEAR("창", "방어력 관통 & 출혈"),
    TRIDENT("삼지창", "환영 삼지창 미니언 & 에임 사출"),
    BOW("활", "속사 & 멀티샷 다발 사격"),
    MACE("철퇴", "도약 강타 & 지진 스매시"),
    UNKNOWN("기타", "강화 불가");

    private final String displayName;
    private final String effectDescription;

    EnhanceEquipmentType(String displayName, String effectDescription) {
        this.displayName = displayName;
        this.effectDescription = effectDescription;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEffectDescription() {
        return effectDescription;
    }

    public static EnhanceEquipmentType fromItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return UNKNOWN;
        Material type = item.getType();

        if (type == Material.WOODEN_SWORD
                || type == Material.STONE_SWORD
                || type == Material.IRON_SWORD
                || type == Material.GOLDEN_SWORD
                || type == Material.DIAMOND_SWORD
                || type == Material.NETHERITE_SWORD) {
            return SWORD;
        }

        if (type == Material.WOODEN_AXE
                || type == Material.STONE_AXE
                || type == Material.IRON_AXE
                || type == Material.GOLDEN_AXE
                || type == Material.DIAMOND_AXE
                || type == Material.NETHERITE_AXE) {
            return AXE;
        }

        if (type == Material.WOODEN_PICKAXE
                || type == Material.STONE_PICKAXE
                || type == Material.IRON_PICKAXE
                || type == Material.GOLDEN_PICKAXE
                || type == Material.DIAMOND_PICKAXE
                || type == Material.NETHERITE_PICKAXE) {
            return PICKAXE;
        }

        if (type == Material.BOW) {
            return BOW;
        }

        if (type == Material.MACE) {
            return MACE;
        }

        if (type == Material.TRIDENT) {
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName() && item.getItemMeta().getDisplayName().contains("창")) {
                return SPEAR;
            }
            return TRIDENT;
        }

        return UNKNOWN;
    }
}
