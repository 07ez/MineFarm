package Hez.Enhance;

import Hez.MineFarm.Main;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class EnhanceManager {
    public static final int MAX_LEVEL = 10;
    private static final NamespacedKey ENHANCE_LEVEL_KEY = new NamespacedKey(Main.getInstance(), "enhance_level");
    private static final NamespacedKey ATTACK_DAMAGE_KEY = new NamespacedKey(Main.getInstance(), "tool_base_damage");
    private static final NamespacedKey ATTACK_SPEED_KEY = new NamespacedKey(Main.getInstance(), "tool_attack_speed");
    private static final NamespacedKey MINING_SPEED_KEY = new NamespacedKey(Main.getInstance(), "tool_mining_speed");

    // [검] 1강부터 10강까지 설정 테이블 (실패 시 모두 단계 유지)
    private static final Map<Integer, EnhanceStepConfig> SWORD_CONFIGS = new LinkedHashMap<>();

    // [도끼] 1강부터 10강까지 찹트리 설정 테이블 (실패 시 모두 단계 유지)
    private static final Map<Integer, AxeChopConfig> AXE_CONFIGS = new LinkedHashMap<>();

    // [곡괭이] 1강부터 10강까지 채굴 속도 & 범위 채굴 설정 테이블 (실패 시 모두 단계 유지)
    private static final Map<Integer, PickaxeMiningConfig> PICKAXE_CONFIGS = new LinkedHashMap<>();

    // [창] 1강부터 10강까지 방어력 관통 & 출혈 설정 테이블 (실패 시 모두 단계 유지)
    private static final Map<Integer, SpearBleedConfig> SPEAR_CONFIGS = new LinkedHashMap<>();

    // [활] 1강부터 10강까지 속사 & 멀티샷 설정 테이블 (실패 시 모두 단계 유지)
    private static final Map<Integer, BowRapidConfig> BOW_CONFIGS = new LinkedHashMap<>();

    // [삼지창] 1강부터 10강까지 미니언 수 증가 설정 테이블 (실패 시 모두 단계 유지)
    private static final Map<Integer, TridentMinionConfig> TRIDENT_CONFIGS = new LinkedHashMap<>();

    // [철퇴] 1강부터 10강까지 도약 강타 & 지진 스매시 설정 테이블 (실패 시 모두 단계 유지)
    private static final Map<Integer, MaceSmashConfig> MACE_CONFIGS = new LinkedHashMap<>();

    static {
        // --- [검] (targetLevel, successRate, failureRate, destroyRate, bonusAttackSpeed, cost) ---
        SWORD_CONFIGS.put(1,  new EnhanceStepConfig(1,  100.0,  0.0,  0.0, 0.10, 1000));
        SWORD_CONFIGS.put(2,  new EnhanceStepConfig(2,   95.0,  5.0,  0.0, 0.20, 2000));
        SWORD_CONFIGS.put(3,  new EnhanceStepConfig(3,   90.0, 10.0,  0.0, 0.30, 3000));
        SWORD_CONFIGS.put(4,  new EnhanceStepConfig(4,   80.0, 20.0,  0.0, 0.45, 5000));
        SWORD_CONFIGS.put(5,  new EnhanceStepConfig(5,   70.0, 30.0,  0.0, 0.60, 8000));
        SWORD_CONFIGS.put(6,  new EnhanceStepConfig(6,   60.0, 37.0,  3.0, 0.80, 12000));
        SWORD_CONFIGS.put(7,  new EnhanceStepConfig(7,   50.0, 43.0,  7.0, 1.00, 18000));
        SWORD_CONFIGS.put(8,  new EnhanceStepConfig(8,   40.0, 48.0, 12.0, 1.20, 25000));
        SWORD_CONFIGS.put(9,  new EnhanceStepConfig(9,   30.0, 52.0, 18.0, 1.40, 35000));
        SWORD_CONFIGS.put(10, new EnhanceStepConfig(10,  20.0, 55.0, 25.0, 1.65, 50000));

        // --- [도끼] 찹트리 설정 (targetLevel, success, fail, destroy, maxBlocks, cooldownSeconds, breakLeaves, saveDurabilityRate, cost) ---
        AXE_CONFIGS.put(1,  new AxeChopConfig(1,  100.0,  0.0,  0.0,   6, 5.0, false,  0.0, 1000));
        AXE_CONFIGS.put(2,  new AxeChopConfig(2,   95.0,  5.0,  0.0,  12, 4.5, false,  5.0, 2000));
        AXE_CONFIGS.put(3,  new AxeChopConfig(3,   90.0, 10.0,  0.0,  20, 4.0, false, 10.0, 3000));
        AXE_CONFIGS.put(4,  new AxeChopConfig(4,   80.0, 20.0,  0.0,  30, 3.5, false, 15.0, 5000));
        AXE_CONFIGS.put(5,  new AxeChopConfig(5,   70.0, 30.0,  0.0,  45, 3.0, false, 20.0, 8000));
        AXE_CONFIGS.put(6,  new AxeChopConfig(6,   60.0, 37.0,  3.0,  65, 2.5, false, 25.0, 12000));
        AXE_CONFIGS.put(7,  new AxeChopConfig(7,   50.0, 43.0,  7.0,  90, 2.0, true,  30.0, 18000));
        AXE_CONFIGS.put(8,  new AxeChopConfig(8,   40.0, 48.0, 12.0, 130, 1.5, true,  35.0, 25000));
        AXE_CONFIGS.put(9,  new AxeChopConfig(9,   30.0, 52.0, 18.0, 180, 1.0, true,  40.0, 35000));
        AXE_CONFIGS.put(10, new AxeChopConfig(10,  20.0, 55.0, 25.0, 260, 0.0, true,  50.0, 50000));

        // --- [곡괭이] (targetLevel, success, fail, destroy, speedBonus, areaType, autoPickup, cost) ---
        PICKAXE_CONFIGS.put(1,  new PickaxeMiningConfig(1,  100.0,  0.0,  0.0, 0.20, AreaMiningType.NONE,           false, 1000));
        PICKAXE_CONFIGS.put(2,  new PickaxeMiningConfig(2,   95.0,  5.0,  0.0, 0.45, AreaMiningType.NONE,           false, 2000));
        PICKAXE_CONFIGS.put(3,  new PickaxeMiningConfig(3,   90.0, 10.0,  0.0, 0.75, AreaMiningType.NONE,           false, 3000));
        PICKAXE_CONFIGS.put(4,  new PickaxeMiningConfig(4,   80.0, 20.0,  0.0, 1.15, AreaMiningType.NONE,           false, 5000));
        PICKAXE_CONFIGS.put(5,  new PickaxeMiningConfig(5,   70.0, 30.0,  0.0, 1.60, AreaMiningType.NONE,           false, 8000));
        PICKAXE_CONFIGS.put(6,  new PickaxeMiningConfig(6,   60.0, 37.0,  3.0, 2.10, AreaMiningType.HORIZONTAL_3x1, false, 12000));
        PICKAXE_CONFIGS.put(7,  new PickaxeMiningConfig(7,   50.0, 43.0,  7.0, 2.70, AreaMiningType.AREA_3x3,       false, 18000));
        PICKAXE_CONFIGS.put(8,  new PickaxeMiningConfig(8,   40.0, 48.0, 12.0, 3.40, AreaMiningType.DEPTH_3x3x2,     false, 25000));
        PICKAXE_CONFIGS.put(9,  new PickaxeMiningConfig(9,   30.0, 52.0, 18.0, 4.20, AreaMiningType.CUBE_3x3x3,      false, 35000));
        PICKAXE_CONFIGS.put(10, new PickaxeMiningConfig(10,  20.0, 55.0, 25.0, 5.00, AreaMiningType.CUBE_3x3x3,      false, 50000));

        // --- [창] (targetLevel, success, fail, destroy, armorPenetrationRate, bleedChance, bleedDuration, damagePerTick, slownessAmp, cost) ---
        SPEAR_CONFIGS.put(1,  new SpearBleedConfig(1,  100.0,  0.0,  0.0, 0.10,   0.0, 0.0, 0.0, -1, 1000));
        SPEAR_CONFIGS.put(2,  new SpearBleedConfig(2,   95.0,  5.0,  0.0, 0.20,   0.0, 0.0, 0.0, -1, 2000));
        SPEAR_CONFIGS.put(3,  new SpearBleedConfig(3,   90.0, 10.0,  0.0, 0.30,   0.0, 0.0, 0.0, -1, 3000));
        SPEAR_CONFIGS.put(4,  new SpearBleedConfig(4,   80.0, 20.0,  0.0, 0.40,   0.0, 0.0, 0.0, -1, 5000));
        SPEAR_CONFIGS.put(5,  new SpearBleedConfig(5,   70.0, 30.0,  0.0, 0.50,   0.0, 0.0, 0.0, -1, 8000));
        SPEAR_CONFIGS.put(6,  new SpearBleedConfig(6,   60.0, 37.0,  3.0, 0.60,  25.0, 2.5, 1.5,  0, 12000));
        SPEAR_CONFIGS.put(7,  new SpearBleedConfig(7,   50.0, 43.0,  7.0, 0.70,  40.0, 3.0, 2.0,  0, 18000));
        SPEAR_CONFIGS.put(8,  new SpearBleedConfig(8,   40.0, 48.0, 12.0, 0.80,  60.0, 3.5, 2.5,  1, 25000));
        SPEAR_CONFIGS.put(9,  new SpearBleedConfig(9,   30.0, 52.0, 18.0, 0.90,  80.0, 4.0, 3.0,  1, 35000));
        SPEAR_CONFIGS.put(10, new SpearBleedConfig(10,  20.0, 55.0, 25.0, 1.00, 100.0, 5.0, 4.0,  2, 50000));

        // --- [활] (targetLevel, success, fail, destroy, chargeBonus, projectileSpeedBonus, arrowCount, pierceLevel, cost) ---
        BOW_CONFIGS.put(1,  new BowRapidConfig(1,  100.0,  0.0,  0.0, 0.15, 0.10, 1, 0, 1000));
        BOW_CONFIGS.put(2,  new BowRapidConfig(2,   95.0,  5.0,  0.0, 0.30, 0.20, 1, 0, 2000));
        BOW_CONFIGS.put(3,  new BowRapidConfig(3,   90.0, 10.0,  0.0, 0.45, 0.30, 1, 0, 3000));
        BOW_CONFIGS.put(4,  new BowRapidConfig(4,   80.0, 20.0,  0.0, 0.60, 0.40, 1, 0, 5000));
        BOW_CONFIGS.put(5,  new BowRapidConfig(5,   70.0, 30.0,  0.0, 0.75, 0.50, 1, 0, 8000));
        BOW_CONFIGS.put(6,  new BowRapidConfig(6,   60.0, 37.0,  3.0, 0.80, 0.55, 3, 0, 12000));
        BOW_CONFIGS.put(7,  new BowRapidConfig(7,   50.0, 43.0,  7.0, 0.85, 0.60, 3, 0, 18000));
        BOW_CONFIGS.put(8,  new BowRapidConfig(8,   40.0, 48.0, 12.0, 0.90, 0.70, 4, 0, 25000));
        BOW_CONFIGS.put(9,  new BowRapidConfig(9,   30.0, 52.0, 18.0, 0.95, 0.80, 5, 0, 35000));
        BOW_CONFIGS.put(10, new BowRapidConfig(10,  20.0, 55.0, 25.0, 1.00, 1.00, 5, 1, 50000));

        // --- [삼지창] (targetLevel, success, fail, destroy, minionCount, cooldownSeconds, damagePerMinion, cost) ---
        TRIDENT_CONFIGS.put(1,  new TridentMinionConfig(1,  100.0,  0.0,  0.0,  1, 3.0,  8.0, 1000));
        TRIDENT_CONFIGS.put(2,  new TridentMinionConfig(2,   95.0,  5.0,  0.0,  2, 3.0,  8.5, 2000));
        TRIDENT_CONFIGS.put(3,  new TridentMinionConfig(3,   90.0, 10.0,  0.0,  3, 2.8,  9.0, 3000));
        TRIDENT_CONFIGS.put(4,  new TridentMinionConfig(4,   80.0, 20.0,  0.0,  4, 2.8,  9.5, 5000));
        TRIDENT_CONFIGS.put(5,  new TridentMinionConfig(5,   70.0, 30.0,  0.0,  5, 2.5, 10.0, 8000));
        TRIDENT_CONFIGS.put(6,  new TridentMinionConfig(6,   60.0, 37.0,  3.0,  6, 2.5, 10.5, 12000));
        TRIDENT_CONFIGS.put(7,  new TridentMinionConfig(7,   50.0, 43.0,  7.0,  7, 2.2, 11.0, 18000));
        TRIDENT_CONFIGS.put(8,  new TridentMinionConfig(8,   40.0, 48.0, 12.0,  8, 2.0, 11.5, 25000));
        TRIDENT_CONFIGS.put(9,  new TridentMinionConfig(9,   30.0, 52.0, 18.0,  9, 1.8, 12.0, 35000));
        TRIDENT_CONFIGS.put(10, new TridentMinionConfig(10,  20.0, 55.0, 25.0, 10, 1.5, 13.0, 50000));

        // --- [철퇴/메이스] (targetLevel, success, fail, destroy, jumpVelocity, cooldownSeconds, earthquakeRadius, earthquakeDamage, airbornePower, cost) ---
        // 1~5강: 도약 높이 및 지진 반경/피해량 증가
        // 6~10강: 광역 에어본 해금 + 반경 11m 대지진 강타
        MACE_CONFIGS.put(1,  new MaceSmashConfig(1,  100.0,  0.0,  0.0, 0.95, 8.0,  3.0,  8.0, 0.0, 1000));
        MACE_CONFIGS.put(2,  new MaceSmashConfig(2,   95.0,  5.0,  0.0, 1.05, 7.5,  3.5, 10.0, 0.0, 2000));
        MACE_CONFIGS.put(3,  new MaceSmashConfig(3,   90.0, 10.0,  0.0, 1.15, 7.0,  4.0, 12.0, 0.0, 3000));
        MACE_CONFIGS.put(4,  new MaceSmashConfig(4,   80.0, 20.0,  0.0, 1.25, 6.5,  4.5, 14.0, 0.0, 5000));
        MACE_CONFIGS.put(5,  new MaceSmashConfig(5,   70.0, 30.0,  0.0, 1.35, 6.0,  5.0, 16.0, 0.0, 8000));
        MACE_CONFIGS.put(6,  new MaceSmashConfig(6,   60.0, 37.0,  3.0, 1.45, 5.5,  6.0, 19.0, 0.6, 12000));
        MACE_CONFIGS.put(7,  new MaceSmashConfig(7,   50.0, 43.0,  7.0, 1.55, 5.0,  7.0, 22.0, 0.7, 18000));
        MACE_CONFIGS.put(8,  new MaceSmashConfig(8,   40.0, 48.0, 12.0, 1.65, 4.5,  8.0, 26.0, 0.8, 25000));
        MACE_CONFIGS.put(9,  new MaceSmashConfig(9,   30.0, 52.0, 18.0, 1.75, 4.0,  9.0, 30.0, 0.9, 35000));
        MACE_CONFIGS.put(10, new MaceSmashConfig(10,  20.0, 55.0, 25.0, 1.85, 3.0, 11.0, 36.0, 1.1, 50000));
    }

    public static EnhanceEquipmentType getEquipmentType(ItemStack item) {
        return EnhanceEquipmentType.fromItem(item);
    }

    public static boolean isEnhanceable(ItemStack item) {
        return getEquipmentType(item) != EnhanceEquipmentType.UNKNOWN;
    }

    public static boolean isSword(ItemStack item) {
        return getEquipmentType(item) == EnhanceEquipmentType.SWORD;
    }

    public static boolean isAxe(ItemStack item) {
        return getEquipmentType(item) == EnhanceEquipmentType.AXE;
    }

    public static boolean isPickaxe(ItemStack item) {
        return getEquipmentType(item) == EnhanceEquipmentType.PICKAXE;
    }

    public static boolean isBow(ItemStack item) {
        return getEquipmentType(item) == EnhanceEquipmentType.BOW;
    }

    public static boolean isSpear(ItemStack item) {
        return getEquipmentType(item) == EnhanceEquipmentType.SPEAR;
    }

    public static boolean isTrident(ItemStack item) {
        return getEquipmentType(item) == EnhanceEquipmentType.TRIDENT;
    }

    public static boolean isMace(ItemStack item) {
        return getEquipmentType(item) == EnhanceEquipmentType.MACE;
    }

    public static int getEnhanceLevel(ItemStack item) {
        if (!isEnhanceable(item)) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.getOrDefault(ENHANCE_LEVEL_KEY, PersistentDataType.INTEGER, 0);
    }

    public static EnhanceStepConfig getSwordConfig(int targetLevel) {
        return SWORD_CONFIGS.get(targetLevel);
    }

    public static AxeChopConfig getAxeConfig(int targetLevel) {
        return AXE_CONFIGS.get(targetLevel);
    }

    public static PickaxeMiningConfig getPickaxeConfig(int targetLevel) {
        return PICKAXE_CONFIGS.get(targetLevel);
    }

    public static SpearBleedConfig getSpearConfig(int targetLevel) {
        return SPEAR_CONFIGS.get(targetLevel);
    }

    public static BowRapidConfig getBowConfig(int targetLevel) {
        return BOW_CONFIGS.get(targetLevel);
    }

    public static TridentMinionConfig getTridentConfig(int targetLevel) {
        return TRIDENT_CONFIGS.get(targetLevel);
    }

    public static MaceSmashConfig getMaceConfig(int targetLevel) {
        return MACE_CONFIGS.get(targetLevel);
    }

    public static EnhanceResult enhance(ItemStack item) {
        EnhanceEquipmentType eqType = getEquipmentType(item);
        if (eqType == EnhanceEquipmentType.UNKNOWN) {
            return new EnhanceResult(EnhanceResultType.INVALID_ITEM, 0, 0, item, "§c강화할 수 없는 아이템입니다. (검, 도끼, 곡괭이, 삼지창, 활, 철퇴만 가능)");
        }

        int currentLevel = getEnhanceLevel(item);
        if (currentLevel >= MAX_LEVEL) {
            return new EnhanceResult(EnhanceResultType.MAX_LEVEL, currentLevel, currentLevel, item, "§c이미 최고 강화 단계(+" + MAX_LEVEL + ")입니다.");
        }

        int targetLevel = currentLevel + 1;
        double successRate;
        double destroyRate;

        if (eqType == EnhanceEquipmentType.SWORD) {
            EnhanceStepConfig cfg = getSwordConfig(targetLevel);
            if (cfg == null) return new EnhanceResult(EnhanceResultType.INVALID_ITEM, currentLevel, currentLevel, item, "§c설정을 찾을 수 없습니다.");
            successRate = cfg.getSuccessRate();
            destroyRate = cfg.getDestroyRate();
        } else if (eqType == EnhanceEquipmentType.AXE) {
            AxeChopConfig cfg = getAxeConfig(targetLevel);
            if (cfg == null) return new EnhanceResult(EnhanceResultType.INVALID_ITEM, currentLevel, currentLevel, item, "§c설정을 찾을 수 없습니다.");
            successRate = cfg.getSuccessRate();
            destroyRate = cfg.getDestroyRate();
        } else if (eqType == EnhanceEquipmentType.PICKAXE) {
            PickaxeMiningConfig cfg = getPickaxeConfig(targetLevel);
            if (cfg == null) return new EnhanceResult(EnhanceResultType.INVALID_ITEM, currentLevel, currentLevel, item, "§c설정을 찾을 수 없습니다.");
            successRate = cfg.getSuccessRate();
            destroyRate = cfg.getDestroyRate();
        } else if (eqType == EnhanceEquipmentType.SPEAR) {
            SpearBleedConfig cfg = getSpearConfig(targetLevel);
            if (cfg == null) return new EnhanceResult(EnhanceResultType.INVALID_ITEM, currentLevel, currentLevel, item, "§c설정을 찾을 수 없습니다.");
            successRate = cfg.getSuccessRate();
            destroyRate = cfg.getDestroyRate();
        } else if (eqType == EnhanceEquipmentType.BOW) {
            BowRapidConfig cfg = getBowConfig(targetLevel);
            if (cfg == null) return new EnhanceResult(EnhanceResultType.INVALID_ITEM, currentLevel, currentLevel, item, "§c설정을 찾을 수 없습니다.");
            successRate = cfg.getSuccessRate();
            destroyRate = cfg.getDestroyRate();
        } else if (eqType == EnhanceEquipmentType.TRIDENT) {
            TridentMinionConfig cfg = getTridentConfig(targetLevel);
            if (cfg == null) return new EnhanceResult(EnhanceResultType.INVALID_ITEM, currentLevel, currentLevel, item, "§c설정을 찾을 수 없습니다.");
            successRate = cfg.getSuccessRate();
            destroyRate = cfg.getDestroyRate();
        } else {
            MaceSmashConfig cfg = getMaceConfig(targetLevel);
            if (cfg == null) return new EnhanceResult(EnhanceResultType.INVALID_ITEM, currentLevel, currentLevel, item, "§c설정을 찾을 수 없습니다.");
            successRate = cfg.getSuccessRate();
            destroyRate = cfg.getDestroyRate();
        }

        double roll = ThreadLocalRandom.current().nextDouble() * 100.0;
        double destroyThreshold = successRate + destroyRate;

        if (roll < successRate) {
            int nextLevel = targetLevel;
            applyEnhanceMeta(item, nextLevel);
            String effectName = switch (eqType) {
                case SWORD -> "휘두르기 쿨타임이 감소";
                case AXE -> "찹트리 능력이 증가";
                case PICKAXE -> "채굴 속도 및 채굴 범위가 확장";
                case SPEAR -> "방어력 관통 및 출혈 능력이 향상";
                case BOW -> "속사 및 멀티샷 화살 사격 능력이 강화";
                case TRIDENT -> "소환되는 삼지창 미니언 수가 증가";
                case MACE -> "도약 높이 및 지진 스매시 충격파가 강화";
                default -> "능력이 향상";
            };
            String msg = "§a★ 강화 성공! §f(§e+" + currentLevel + " §7➔ §6+" + nextLevel + "§f) " + effectName + "했습니다!";
            return new EnhanceResult(EnhanceResultType.SUCCESS, currentLevel, nextLevel, item, msg);
        } else if (roll < destroyThreshold) {
            String msg = "§c☠ 강화 파괴! §7장비가 버티지 못하고 파괴되었습니다... (§c+" + currentLevel + "§7)";
            return new EnhanceResult(EnhanceResultType.DESTROY, currentLevel, 0, null, msg);
        } else {
            String msg = "§e! 강화 실패! §7강화 단계가 유지되었습니다. (§e+" + currentLevel + "§7)";
            return new EnhanceResult(EnhanceResultType.FAILURE, currentLevel, currentLevel, item, msg);
        }
    }

    public static EnhanceResult enhance(Player player, ItemStack item) {
        EnhanceResult result = enhance(item);

        if (player != null && player.isOnline()) {
            player.sendMessage(result.getMessage());

            switch (result.getType()) {
                case SUCCESS -> {
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                    player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
                    if (isTrident(item)) {
                        TridentMinionManager.updatePlayerMinions(player);
                    }
                }
                case FAILURE -> {
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_HIT, 1.0f, 0.8f);
                    player.spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
                }
                case DESTROY -> {
                    if (isTrident(item)) {
                        TridentMinionManager.clearMinions(player);
                    }
                    if (item != null) {
                        item.setAmount(0);
                    }
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.7f);
                    player.spawnParticle(Particle.LAVA, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
                }
                default -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
            }
        }

        return result;
    }

    public static void applyEnhanceMeta(ItemStack item, int level) {
        EnhanceEquipmentType eqType = getEquipmentType(item);
        if (eqType == EnhanceEquipmentType.UNKNOWN) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.getPersistentDataContainer().set(ENHANCE_LEVEL_KEY, PersistentDataType.INTEGER, level);

        if (eqType == EnhanceEquipmentType.SWORD) {
            applySwordAttributes(meta, item.getType(), level);
        } else if (eqType == EnhanceEquipmentType.PICKAXE) {
            applyPickaxeAttributes(meta, item.getType(), level);
        } else if (eqType == EnhanceEquipmentType.TRIDENT || eqType == EnhanceEquipmentType.SPEAR) {
            applyTridentAttributes(meta, level);
        } else if (eqType == EnhanceEquipmentType.MACE) {
            applyMaceAttributes(meta, level);
        }

        updateLore(meta, eqType, level);

        item.setItemMeta(meta);
    }

    private static void applySwordAttributes(ItemMeta meta, Material material, int level) {
        meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
        meta.removeAttributeModifier(Attribute.ATTACK_SPEED);

        double baseDamage = getSwordBaseDamage(material);
        AttributeModifier damageMod = new AttributeModifier(
                ATTACK_DAMAGE_KEY,
                baseDamage - 1.0,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.MAINHAND
        );
        meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, damageMod);

        double bonusSpeed = 0.0;
        if (level > 0 && SWORD_CONFIGS.containsKey(level)) {
            bonusSpeed = SWORD_CONFIGS.get(level).getBonusAttackSpeed();
        }
        double finalSpeedModifier = -2.4 + bonusSpeed;

        AttributeModifier speedMod = new AttributeModifier(
                ATTACK_SPEED_KEY,
                finalSpeedModifier,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.MAINHAND
        );
        meta.addAttributeModifier(Attribute.ATTACK_SPEED, speedMod);
    }

    private static void applyPickaxeAttributes(ItemMeta meta, Material material, int level) {
        meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
        meta.removeAttributeModifier(Attribute.ATTACK_SPEED);
        meta.removeAttributeModifier(Attribute.BLOCK_BREAK_SPEED);

        double baseDamage = getPickaxeBaseDamage(material);
        AttributeModifier damageMod = new AttributeModifier(
                ATTACK_DAMAGE_KEY,
                baseDamage - 1.0,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.MAINHAND
        );
        meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, damageMod);

        AttributeModifier speedMod = new AttributeModifier(
                ATTACK_SPEED_KEY,
                -2.8,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.MAINHAND
        );
        meta.addAttributeModifier(Attribute.ATTACK_SPEED, speedMod);

        if (level > 0 && PICKAXE_CONFIGS.containsKey(level)) {
            double speedBonus = PICKAXE_CONFIGS.get(level).getSpeedBonus();
            AttributeModifier breakSpeedMod = new AttributeModifier(
                    MINING_SPEED_KEY,
                    speedBonus,
                    AttributeModifier.Operation.ADD_SCALAR,
                    EquipmentSlotGroup.MAINHAND
            );
            meta.addAttributeModifier(Attribute.BLOCK_BREAK_SPEED, breakSpeedMod);
        }
    }

    private static void applyTridentAttributes(ItemMeta meta, int level) {
        meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
        meta.removeAttributeModifier(Attribute.ATTACK_SPEED);

        AttributeModifier damageMod = new AttributeModifier(
                ATTACK_DAMAGE_KEY,
                8.0,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.MAINHAND
        );
        meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, damageMod);

        AttributeModifier speedMod = new AttributeModifier(
                ATTACK_SPEED_KEY,
                -2.9,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.MAINHAND
        );
        meta.addAttributeModifier(Attribute.ATTACK_SPEED, speedMod);
    }

    private static void applyMaceAttributes(ItemMeta meta, int level) {
        meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
        meta.removeAttributeModifier(Attribute.ATTACK_SPEED);

        // 메이스 기본 공격력: 6.0, 공격속도 0.6 (-3.4)
        AttributeModifier damageMod = new AttributeModifier(
                ATTACK_DAMAGE_KEY,
                5.0,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.MAINHAND
        );
        meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, damageMod);

        AttributeModifier speedMod = new AttributeModifier(
                ATTACK_SPEED_KEY,
                -3.4,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.MAINHAND
        );
        meta.addAttributeModifier(Attribute.ATTACK_SPEED, speedMod);
    }

    public static double getSwordBaseDamage(Material material) {
        return switch (material) {
            case WOODEN_SWORD, GOLDEN_SWORD -> 4.0;
            case STONE_SWORD -> 5.0;
            case IRON_SWORD -> 6.0;
            case DIAMOND_SWORD -> 7.0;
            case NETHERITE_SWORD -> 8.0;
            default -> 4.0;
        };
    }

    public static double getPickaxeBaseDamage(Material material) {
        return switch (material) {
            case WOODEN_PICKAXE, GOLDEN_PICKAXE -> 2.0;
            case STONE_PICKAXE -> 3.0;
            case IRON_PICKAXE -> 4.0;
            case DIAMOND_PICKAXE -> 5.0;
            case NETHERITE_PICKAXE -> 6.0;
            default -> 2.0;
        };
    }

    private static void updateLore(ItemMeta meta, EnhanceEquipmentType type, int level) {
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        lore.removeIf(line -> line.contains("강화 단계") || line.contains("휘두르기 쿨타임")
                || line.contains("공격 속도:") || line.contains("찹트리") || line.contains("내구도 보호")
                || line.contains("채굴 가속") || line.contains("범위 채굴") || line.contains("자동 수거")
                || line.contains("속사 가속") || line.contains("멀티샷")
                || line.contains("방어력 관통") || line.contains("출혈")
                || line.contains("삼지창 미니언") || line.contains("에임 사출")
                || line.contains("도약 강타") || line.contains("지진 스매시") || line.contains("광역 에어본")
                || line.equals("§8───────────────"));

        if (level > 0) {
            lore.add("§8───────────────");
            lore.add("§6★ 강화 단계: §e+" + level + " §7/ §c" + MAX_LEVEL);

            if (type == EnhanceEquipmentType.SWORD) {
                EnhanceStepConfig config = SWORD_CONFIGS.get(level);
                double bonusSpeed = config != null ? config.getBonusAttackSpeed() : 0.0;
                double reduction = config != null ? config.getCooldownReductionPercentage() : 0.0;
                lore.add(String.format("§b⚡ 공격 속도: §f+%.2f §a(휘두르기 쿨타임 -%.1f%%)", bonusSpeed, reduction));
            } else if (type == EnhanceEquipmentType.AXE) {
                AxeChopConfig config = AXE_CONFIGS.get(level);
                if (config != null) {
                    lore.add(String.format("§a🪓 찹트리: §f최대 §e%d개 §f연쇄 벌목", config.getMaxBlocks()));
                    String cdText = config.getCooldownSeconds() > 0 ? String.format("%.1f초", config.getCooldownSeconds()) : "§b노쿨타임";
                    lore.add(String.format("§7- 쿨타임: §f%s §7| 나뭇잎 파괴: %s", cdText, config.isBreakLeaves() ? "§aO" : "§cX"));
                    if (config.getSaveDurabilityRate() > 0) {
                        lore.add(String.format("§7- 내구도 보호: §e%.0f%% 확률 미소모", config.getSaveDurabilityRate()));
                    }
                }
            } else if (type == EnhanceEquipmentType.PICKAXE) {
                PickaxeMiningConfig config = PICKAXE_CONFIGS.get(level);
                if (config != null) {
                    lore.add(String.format("§e⚡ 채굴 가속: §a+%d%% §7(인챈트 중첩 가능)", config.getSpeedPercent()));
                    if (config.getAreaType().isAreaMining()) {
                        lore.add(String.format("§6⛏ 범위 채굴: §f%s §7(Shift 시 1칸 단일 채굴)", config.getAreaType().getDisplayName()));
                    }
                    if (config.isAutoPickup()) {
                        lore.add("§d🧲 드롭 아이템 인벤토리 즉시 자동 수거");
                    }
                }
            } else if (type == EnhanceEquipmentType.BOW) {
                BowRapidConfig config = BOW_CONFIGS.get(level);
                if (config != null) {
                    lore.add(String.format("§e🏹 속사 가속: §a+%d%% §7(초고속 풀차징 & 탄속 +%d%%)",
                            config.getChargePercent(), (int) (config.getProjectileSpeedBonus() * 100)));
                    if (config.isMultiShot()) {
                        String pierceText = config.getPierceLevel() > 0 ? " & 관통" : "";
                        lore.add(String.format("§b🎯 멀티샷: §f화살 §e%d발 §f동시 사격%s", config.getArrowCount(), pierceText));
                    }
                }
            } else if (type == EnhanceEquipmentType.SPEAR) {
                SpearBleedConfig config = SPEAR_CONFIGS.get(level);
                if (config != null) {
                    lore.add(String.format("§c🗡 방어력 관통: §f+%d%%", config.getArmorPenetrationPercent()));
                    if (config.getBleedChance() > 0) {
                        lore.add(String.format("§4🩸 출혈 부여: §e%.0f%% 확률", config.getBleedChance()));
                    }
                }
            } else if (type == EnhanceEquipmentType.TRIDENT) {
                TridentMinionConfig config = TRIDENT_CONFIGS.get(level);
                if (config != null) {
                    lore.add(String.format("§b🔱 삼지창 미니언: §e%d자루 §f소환", config.getMinionCount()));
                    lore.add(String.format("§7- 우클릭 차징 후 해제 시 에임 방향 일제 사출"));
                    lore.add(String.format("§7- 재장전 쿨타임: §f%.1f초 §7| 자루당 피해: §c%.1f",
                            config.getCooldownSeconds(), config.getDamagePerMinion()));
                }
            } else if (type == EnhanceEquipmentType.MACE) {
                MaceSmashConfig config = MACE_CONFIGS.get(level);
                if (config != null) {
                    lore.add(String.format("§6🔨 도약 강타: §e우클릭 시 하늘로 슈퍼 점프"));
                    String airText = config.hasAirborne() ? " & 광역 에어본" : "";
                    lore.add(String.format("§7- 착지 시 반경 §e%.1fm §7지진 스매시 (피해: §c%.1f%s§7)",
                            config.getEarthquakeRadius(), config.getEarthquakeDamage(), airText));
                    lore.add(String.format("§7- 쿨타임: §f%.1f초 §7(도약 낙하 데미지 완전 면제)", config.getCooldownSeconds()));
                }
            }

            lore.add("§8───────────────");
        }

        meta.setLore(lore);
    }

    public static ItemStack createEnhancedSword(Material material, int level) {
        ItemStack item = new ItemStack(material);
        applyEnhanceMeta(item, level);
        return item;
    }

    public static ItemStack createEnhancedAxe(Material material, int level) {
        ItemStack item = new ItemStack(material);
        applyEnhanceMeta(item, level);
        return item;
    }

    public static ItemStack createEnhancedPickaxe(Material material, int level) {
        ItemStack item = new ItemStack(material);
        applyEnhanceMeta(item, level);
        return item;
    }

    public static ItemStack createEnhancedBow(Material material, int level) {
        ItemStack item = new ItemStack(material);
        applyEnhanceMeta(item, level);
        return item;
    }

    public static ItemStack createEnhancedTrident(int level) {
        ItemStack item = new ItemStack(Material.TRIDENT);
        applyEnhanceMeta(item, level);
        return item;
    }

    public static ItemStack createEnhancedSpear(int level) {
        ItemStack item = new ItemStack(Material.TRIDENT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§b강화된 창");
            meta.setItemModel(org.bukkit.NamespacedKey.minecraft("00_spear"));
            try {
                org.bukkit.inventory.meta.components.CustomModelDataComponent cmd = meta.getCustomModelDataComponent();
                cmd.setStrings(java.util.List.of("00_spear"));
                meta.setCustomModelDataComponent(cmd);
            } catch (Throwable ignored) {}
            item.setItemMeta(meta);
        }
        applyEnhanceMeta(item, level);
        return item;
    }

    public static ItemStack createEnhancedSpear(Material material, int level) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setItemModel(org.bukkit.NamespacedKey.minecraft("00_spear"));
            try {
                org.bukkit.inventory.meta.components.CustomModelDataComponent cmd = meta.getCustomModelDataComponent();
                cmd.setStrings(java.util.List.of("00_spear"));
                meta.setCustomModelDataComponent(cmd);
            } catch (Throwable ignored) {}
            item.setItemMeta(meta);
        }
        applyEnhanceMeta(item, level);
        return item;
    }

    public static ItemStack createEnhancedMace(int level) {
        ItemStack item = new ItemStack(Material.MACE);
        applyEnhanceMeta(item, level);
        return item;
    }
}
