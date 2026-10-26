package Hez.PvP.Class;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.FoodProperties;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
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
    FOOD_FIGHTER("푸드파이터", "무기: 맨손, 방어구: 철 세트, 능력: 모든 사물을 섭취하여 특수 효과 발동"),
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

                // 푸드파이터 특별 음식 아이템 8종
                // 1. 기반암: 섭취시간 20초 (저항 3 20초, 힘 15 10초)
                inv.setItem(0, createEdibleItem(Material.BEDROCK, 1, "§8기반암", 20.0f, ItemUseAnimation.EAT,
                        List.of("§7섭취 시간: 20초", "§f효과: 저항 III (20초), 힘 XV (10초)")));

                // 2. 깃털: 섭취시간 0.5초 (공중부양 1 10초)
                inv.setItem(1, createEdibleItem(Material.FEATHER, 1, "§f깃털", 0.5f, ItemUseAnimation.EAT,
                        List.of("§7섭취 시간: 0.5초", "§b효과: 공중부양 I (10초)")));

                // 3. 용암양동이: 섭취시간 2초 (내가 불붙음)
                inv.setItem(2, createEdibleItem(Material.LAVA_BUCKET, 1, "§c용암 양동이", 2.0f, ItemUseAnimation.DRINK,
                        List.of("§7섭취 시간: 2초", "§c효과: 자신에게 화염 부착")));

                // 4. 유리: 섭취시간 2초 (투명화 30초)
                inv.setItem(3, createEdibleItem(Material.GLASS, 1, "§f유리", 2.0f, ItemUseAnimation.EAT,
                        List.of("§7섭취 시간: 2초", "§b효과: 투명화 (30초)")));

                // 5. 유황: 섭취시간 1.5초 (멀미 10초, 독 1초)
                inv.setItem(4, createEdibleItem(Material.SULFUR, 1, "§e유황", 1.5f, ItemUseAnimation.EAT,
                        List.of("§7섭취 시간: 1.5초", "§8효과: 멀미 (10초), 독 (1초)")));

                // 6. 화염구: 섭취시간 1초 (5개) (바라보는 방향으로 화염구 발사)
                inv.setItem(5, createEdibleItem(Material.FIRE_CHARGE, 5, "§6화염구", 1.0f, ItemUseAnimation.EAT,
                        List.of("§7섭취 시간: 1초", "§6효과: 바라보는 방향으로 화염구 발사")));

                // 7. 다이아몬드: 섭취시간 1.5초 (발광 30초)
                inv.setItem(6, createEdibleItem(Material.DIAMOND, 1, "§b다이아몬드", 1.5f, ItemUseAnimation.EAT,
                        List.of("§7섭취 시간: 1.5초", "§e효과: 발광 (30초)")));

                // 8. 커맨드 블록: 섭취시간 50초 (상대 순차 처치 즉시 승리)
                inv.setItem(7, createEdibleItem(Material.COMMAND_BLOCK, 1, "§d커맨드 블록", 50.0f, ItemUseAnimation.EAT,
                        List.of("§7섭취 시간: 50초", "§d효과: 상대 팀 전원 처치 및 결투 즉시 승리!")));
            }
            case TELEPORTER -> {
                // 철검 + 사슬 세트
                inv.setItem(0, new ItemStack(Material.IRON_SWORD));
                inv.setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
                inv.setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
                inv.setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
                inv.setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));

                // 텔레포터 특별 아이템: 청금석
                ItemStack lapis = new ItemStack(Material.LAPIS_LAZULI, 1);
                ItemMeta lapisMeta = lapis.getItemMeta();
                if (lapisMeta != null) {
                    lapisMeta.setDisplayName("§9[스킬] §b공간 이동");
                    lapisMeta.setLore(List.of(
                            "§7조작: §eF키 §7(양손 들기)",
                            "§7바라보는 방향으로 §b8블록 §7순간이동합니다.",
                            "§c소모: 배고픔 7"
                    ));
                    lapis.setItemMeta(lapisMeta);
                }
                inv.setItem(1, lapis);
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

    private static ItemStack createEdibleItem(Material material, int amount, String name, float seconds, ItemUseAnimation animation, List<String> lore) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            item.setItemMeta(meta);
        }
        item.setData(DataComponentTypes.FOOD, FoodProperties.food().canAlwaysEat(true).nutrition(1).saturation(1.0f).build());
        item.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
                .consumeSeconds(seconds)
                .animation(animation)
                .hasConsumeParticles(true)
                .build());
        return item;
    }
}
