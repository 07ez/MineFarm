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

        // 1. 커스텀 아이템 '00_spear' (창) 최우선 판별
        if (item.hasItemMeta()) {
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            // (1) 1.21.4+ ItemModel 컴포넌트 검사
            if (meta.hasItemModel()) {
                String modelStr = meta.getItemModel().asString().toLowerCase();
                String modelKey = meta.getItemModel().getKey().toLowerCase();
                if (modelStr.contains("00_spear") || modelKey.contains("00_spear") || modelKey.equals("spear")) {
                    return SPEAR;
                }
            }
            // (2) CustomModelDataComponent의 strings 검사
            if (meta.hasCustomModelDataComponent()) {
                org.bukkit.inventory.meta.components.CustomModelDataComponent comp = meta.getCustomModelDataComponent();
                if (comp.getStrings().stream().anyMatch(s -> s.equalsIgnoreCase("00_spear") || s.toLowerCase().contains("spear"))) {
                    return SPEAR;
                }
            }
            // (3) 삼지창이면서 이름에 "창"이 들어간 경우
            if (item.getType() == Material.TRIDENT && meta.hasDisplayName() && meta.getDisplayName().contains("창")) {
                return SPEAR;
            }
            // (4) 디스플레이 이름이나 로어에 00_spear 가 들어간 경우
            if (meta.hasDisplayName() && meta.getDisplayName().toLowerCase().contains("00_spear")) {
                return SPEAR;
            }
            if (meta.hasLore()) {
                for (String line : meta.getLore()) {
                    if (line.toLowerCase().contains("00_spear")) {
                        return SPEAR;
                    }
                }
            }
        }

        Material type = item.getType();
        if (type.name().toLowerCase().contains("spear")) {
            return SPEAR;
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
