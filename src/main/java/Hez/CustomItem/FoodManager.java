package Hez.CustomItem;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.inventory.meta.components.FoodComponent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * 커스텀 요리 19종의 배고픔, 포화도, 버프 및 툴팁(Lore)을 통합 관리하는 클래스
 */
public class FoodManager {

    public record BuffSpec(PotionEffectType type, int amplifier, int durationSeconds, String koreanDisplay) {
        public PotionEffect toPotionEffect() {
            return new PotionEffect(type, durationSeconds * 20, amplifier, true, true, true);
        }
    }

    public record FoodSpec(
            String modelData,
            String name,
            int nutrition,
            float saturation,
            List<BuffSpec> buffs,
            boolean canAlwaysEat
    ) {}

    private static final Map<String, FoodSpec> FOOD_SPECS = new HashMap<>();

    static {
        // 1. 계란 후라이 (1009): 4 / 2.0 / 효과 없음
        register("1009", "계란 후라이", 4, 2.0f, List.of(), false);

        // 2. 오믈렛 (1015): 6 / 4.0 / 재생 1 (5초)
        register("1015", "오믈렛", 6, 4.0f, List.of(
                new BuffSpec(PotionEffectType.REGENERATION, 0, 5, "재생 I (5초)")
        ), false);

        // 3. 팬케이크 (1016): 6 / 4.0 / 신속 1 (5초)
        register("1016", "팬케이크", 6, 4.0f, List.of(
                new BuffSpec(PotionEffectType.SPEED, 0, 5, "신속 I (5초)")
        ), false);

        // 4. 생선구이 (1003): 6 / 6.0 / 효과 없음
        register("1003", "생선구이", 6, 6.0f, List.of(), false);

        // 5. 회 (1021): 4 / 2.0 / 효과 없음
        register("1021", "회", 4, 2.0f, List.of(), false);

        // 6. 연어 회 (1020): 6 / 5.0 / 수중 호흡 1 (30초)
        register("1020", "연어 회", 6, 5.0f, List.of(
                new BuffSpec(PotionEffectType.WATER_BREATHING, 0, 30, "수중 호흡 I (30초)")
        ), false);

        // 7. 우럭 튀김 (1005): 7 / 6.0 / 저항 1 (15초)
        register("1005", "우럭 튀김", 7, 6.0f, List.of(
                new BuffSpec(PotionEffectType.RESISTANCE, 0, 15, "저항 I (15초)")
        ), false);

        // 8. 오징어 튀김 (1007): 8 / 6.0 / 야간 투시 1 (20초)
        register("1007", "오징어 튀김", 8, 6.0f, List.of(
                new BuffSpec(PotionEffectType.NIGHT_VISION, 0, 20, "야간 투시 I (20초)")
        ), false);

        // 9. 장어 튀김 (1008): 8 / 4.0 / 힘 1, 재생 1, 저항 1 (15초)
        register("1008", "장어 튀김", 8, 4.0f, List.of(
                new BuffSpec(PotionEffectType.STRENGTH, 0, 15, "힘 I (15초)"),
                new BuffSpec(PotionEffectType.REGENERATION, 0, 15, "재생 I (15초)"),
                new BuffSpec(PotionEffectType.RESISTANCE, 0, 15, "저항 I (15초)")
        ), false);

        // 10. 샐러드 (1019): 6 / 6.0 / 신속 3, 재생 3, 점프 5 (30초) + 야간 투시 1 (60초)
        register("1019", "샐러드", 6, 6.0f, List.of(
                new BuffSpec(PotionEffectType.SPEED, 2, 30, "신속 III (30초)"),
                new BuffSpec(PotionEffectType.REGENERATION, 2, 30, "재생 III (30초)"),
                new BuffSpec(PotionEffectType.JUMP_BOOST, 4, 30, "점프 강화 V (30초)"),
                new BuffSpec(PotionEffectType.NIGHT_VISION, 0, 60, "야간 투시 I (1분)")
        ), true);

        // 11. 해쉬브라운 (1012): 6 / 5.0 / 신속 2 (20초)
        register("1012", "해쉬브라운", 6, 5.0f, List.of(
                new BuffSpec(PotionEffectType.SPEED, 1, 20, "신속 II (20초)")
        ), false);

        // 12. 버섯구이 (1010): 6 / 3.0 / 효과 없음
        register("1010", "버섯구이", 6, 3.0f, List.of(), false);

        // 13. 송어 스프 (1022): 8 / 6.0 / 수중 호흡 1 (1분), 성급함 1 (30초)
        register("1022", "송어 스프", 8, 6.0f, List.of(
                new BuffSpec(PotionEffectType.WATER_BREATHING, 0, 60, "수중 호흡 I (1분)"),
                new BuffSpec(PotionEffectType.HASTE, 0, 30, "성급함 I (30초)")
        ), false);

        // 14. 설탕 당근 스프 (1017): 7 / 5.0 / 신속 5 (20초), 성급함 3 (15초)
        register("1017", "설탕 당근 스프", 7, 5.0f, List.of(
                new BuffSpec(PotionEffectType.SPEED, 4, 20, "신속 V (20초)"),
                new BuffSpec(PotionEffectType.HASTE, 2, 15, "성급함 III (15초)")
        ), true);

        // 15. 바다의 요리 (1006): 8 / 8.0 / 행운 1 (1분), 수중 호흡 1 (1분)
        register("1006", "바다의 요리", 8, 8.0f, List.of(
                new BuffSpec(PotionEffectType.LUCK, 0, 60, "행운 I (1분)"),
                new BuffSpec(PotionEffectType.WATER_BREATHING, 0, 60, "수중 호흡 I (1분)")
        ), true);

        // 16. 햄버거 (1002): 16 / 10.0 / 힘 3, 저항 2 (30초)
        register("1002", "햄버거", 16, 10.0f, List.of(
                new BuffSpec(PotionEffectType.STRENGTH, 2, 30, "힘 III (30초)"),
                new BuffSpec(PotionEffectType.RESISTANCE, 1, 30, "저항 II (30초)")
        ), false);

        // 17. 김밥 (1011): 10 / 8.0 / 저항 2, 재생 3 (20초)
        register("1011", "김밥", 10, 8.0f, List.of(
                new BuffSpec(PotionEffectType.RESISTANCE, 1, 20, "저항 II (20초)"),
                new BuffSpec(PotionEffectType.REGENERATION, 2, 20, "재생 III (20초)")
        ), false);

        // 18. 고기 타코 (1014): 20 / 18.0 / 힘 2 (45초), 저항 1 (30초)
        register("1014", "고기 타코", 20, 18.0f, List.of(
                new BuffSpec(PotionEffectType.STRENGTH, 1, 45, "힘 II (45초)"),
                new BuffSpec(PotionEffectType.RESISTANCE, 0, 30, "저항 I (30초)")
        ), false);

        // 19. 완벽한 아침 (1004 & 1018): 20 / 20.0 / 올스탯 3단계 (3분)
        List<BuffSpec> allStatsBuffs = List.of(
                new BuffSpec(PotionEffectType.STRENGTH, 2, 180, "힘 III (3분)"),
                new BuffSpec(PotionEffectType.SPEED, 2, 180, "신속 III (3분)"),
                new BuffSpec(PotionEffectType.RESISTANCE, 2, 180, "저항 III (3분)"),
                new BuffSpec(PotionEffectType.REGENERATION, 2, 180, "재생 III (3분)"),
                new BuffSpec(PotionEffectType.HASTE, 2, 180, "성급함 III (3분)"),
                new BuffSpec(PotionEffectType.JUMP_BOOST, 2, 180, "점프 강화 III (3분)"),
                new BuffSpec(PotionEffectType.ABSORPTION, 2, 180, "흡수 III (3분)"),
                new BuffSpec(PotionEffectType.LUCK, 2, 180, "행운 III (3분)"),
                new BuffSpec(PotionEffectType.NIGHT_VISION, 0, 180, "야간 투시 (3분)"),
                new BuffSpec(PotionEffectType.WATER_BREATHING, 0, 180, "수중 호흡 (3분)"),
                new BuffSpec(PotionEffectType.FIRE_RESISTANCE, 0, 180, "화염 저항 (3분)")
        );
        register("1004", "완벽한 아침", 20, 20.0f, allStatsBuffs, true);
        register("1018", "완벽한 아침", 20, 20.0f, allStatsBuffs, true);

        // 기타: 아이스크림 (1013) - 기본 간식
        register("1013", "아이스크림", 4, 3.0f, List.of(
                new BuffSpec(PotionEffectType.FIRE_RESISTANCE, 0, 15, "화염 저항 I (15초)")
        ), true);
    }

    private static void register(String modelData, String name, int nutrition, float saturation, List<BuffSpec> buffs, boolean canAlwaysEat) {
        FOOD_SPECS.put(modelData, new FoodSpec(modelData, name, nutrition, saturation, buffs, canAlwaysEat));
    }

    public static FoodSpec getFoodSpec(String modelData) {
        return FOOD_SPECS.get(modelData);
    }

    public static FoodSpec getFoodSpec(ItemStack item) {
        if (item == null || item.getType() != Material.APPLE || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        CustomModelDataComponent cmd = meta.getCustomModelDataComponent();
        List<String> strings = cmd.getStrings();
        if (strings == null || strings.isEmpty()) return null;

        return FOOD_SPECS.get(strings.get(0));
    }

    /**
     * 아이템 메타에 FoodComponent(배고픔/포화도) 및 설명(Lore) 적용
     */
    public static void applyFoodMeta(ItemStack item, String customModelData) {
        if (item.getType() != Material.APPLE || customModelData == null) return;

        // 밀가루(1001)는 요리 재료이므로 섭취 불가 처리
        if ("1001".equals(customModelData)) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setLore(List.of("§7다양한 요리에 사용되는 고운 밀가루입니다.", "§8[요리 재료]"));
                item.setItemMeta(meta);
            }
            return;
        }

        FoodSpec spec = FOOD_SPECS.get(customModelData);
        if (spec == null) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // 1. Paper FoodComponent 설정 (네이티브 배고픔 및 노란색 포화도 게이지 직접 적용)
        try {
            FoodComponent food = meta.getFood();
            food.setNutrition(spec.nutrition());
            food.setSaturation(spec.saturation());
            food.setCanAlwaysEat(spec.canAlwaysEat() || !spec.buffs().isEmpty());
            meta.setFood(food);
        } catch (Throwable ignored) {
            // 하위 호환 안전장치 (FoodListener에서도 수동 보정함)
        }

        // 2. 직관적인 툴팁(Lore) 구성
        List<String> lore = new ArrayList<>();
        lore.add(String.format("§7🍖 배고픔 회복: §e+%d §8(%.1f칸)", spec.nutrition(), spec.nutrition() / 2.0));
        lore.add(String.format("§7✨ 포화도(유지력): §6+%.1f", spec.saturation()));

        if (!spec.buffs().isEmpty()) {
            lore.add("");
            lore.add("§b§l[섭취 효과]");
            for (BuffSpec buff : spec.buffs()) {
                lore.add("§f• §a" + buff.koreanDisplay());
            }
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }
}
