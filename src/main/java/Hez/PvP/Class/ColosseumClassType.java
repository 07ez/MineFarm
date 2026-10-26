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
    MOLE("두더지", "무기: 철검, 방어구: 없음, 능력: 호퍼(땅굴로 들어가기) F키 사용 시 땅속 잠입");

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
     * 콜로세움 전투 시작 시 해당 직업의 기본 키트(무기, 방어구, 특별 아이템) 지급
     */
    public void applyKit(Player player) {
        if (player == null) return;
        PlayerInventory inv = player.getInventory();
        inv.clear();

        // 모든 직업 공통 스테이크 10개 지급 (슬롯 8)
        inv.setItem(8, new ItemStack(Material.COOKED_BEEF, 10));

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

                // 파머 특별 아이템: 감자 1, 밀 씨앗 1, 당근 1, 뼛가루 30개
                inv.setItem(1, new ItemStack(Material.POTATO, 1));
                inv.setItem(2, new ItemStack(Material.WHEAT_SEEDS, 1));
                inv.setItem(3, new ItemStack(Material.CARROT, 1));
                inv.setItem(4, new ItemStack(Material.BONE_MEAL, 30));
            }
            case FOOD_FIGHTER -> {
                // 맨손 + 철 세트
                inv.setHelmet(new ItemStack(Material.IRON_HELMET));
                inv.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                inv.setLeggings(new ItemStack(Material.IRON_LEGGINGS));
                inv.setBoots(new ItemStack(Material.IRON_BOOTS));

                // 푸드파이터 특별 아이템: 기반암, 깃털, 용암양동이, 유리, 유황, 화염구, 다이아몬드, 커맨드 블록
                inv.setItem(0, new ItemStack(Material.BEDROCK, 1));
                inv.setItem(1, new ItemStack(Material.FEATHER, 1));
                inv.setItem(2, new ItemStack(Material.LAVA_BUCKET, 1));
                inv.setItem(3, new ItemStack(Material.GLASS, 1));

                // 유황 (바닐라 SULFUR 또는 GUNPOWDER 호환 처리)
                Material sulfurMat = Material.matchMaterial("SULFUR");
                if (sulfurMat == null) sulfurMat = Material.matchMaterial("GUNPOWDER");
                ItemStack sulfurItem = new ItemStack(sulfurMat != null ? sulfurMat : Material.GUNPOWDER, 1);
                ItemMeta sulfurMeta = sulfurItem.getItemMeta();
                if (sulfurMeta != null) {
                    sulfurMeta.setDisplayName("§e유황");
                    sulfurItem.setItemMeta(sulfurMeta);
                }
                inv.setItem(4, sulfurItem);

                inv.setItem(5, new ItemStack(Material.FIRE_CHARGE, 1));
                inv.setItem(6, new ItemStack(Material.DIAMOND, 1));
                inv.setItem(7, new ItemStack(Material.COMMAND_BLOCK, 1));
            }
            case TELEPORTER -> {
                // 철검 + 사슬 세트
                inv.setItem(0, new ItemStack(Material.IRON_SWORD));
                inv.setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
                inv.setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
                inv.setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
                inv.setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));

                // 텔레포터 특별 아이템: 청금석
                inv.setItem(1, new ItemStack(Material.LAPIS_LAZULI, 1));
            }
            case MOLE -> {
                // 철검 + 갑옷 없음 (투명화 시 갑옷이 보이지 않도록 갑옷 제거)
                inv.setItem(0, new ItemStack(Material.IRON_SWORD));

                // 두더지 특수 아이템: 호퍼 (이름: 땅굴로 들어가기)
                ItemStack hopper = new ItemStack(Material.HOPPER, 1);
                ItemMeta meta = hopper.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§6[스킬] §e땅굴로 들어가기");
                    hopper.setItemMeta(meta);
                }
                inv.setItem(1, hopper);
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
