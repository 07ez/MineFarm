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

        // 1. 창 (마인크래프트 26.3 바닐라 창 및 SPEAR 계열)
        if (type == Material.WOODEN_SPEAR
                || type == Material.STONE_SPEAR
                || type == Material.COPPER_SPEAR
                || type == Material.IRON_SPEAR
                || type == Material.GOLDEN_SPEAR
                || type == Material.DIAMOND_SPEAR
                || type == Material.NETHERITE_SPEAR
                || type.name().toLowerCase().contains("spear")) {
            return SPEAR;
        }

        if (item.hasItemMeta()) {
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            // 삼지창이면서 이름에 "창"이 들어간 경우 호환 지원
            if (type == Material.TRIDENT && meta.hasDisplayName() && meta.getDisplayName().contains("창")) {
                return SPEAR;
            }
        }

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
            return TRIDENT;
        }

        return UNKNOWN;
    }
}
