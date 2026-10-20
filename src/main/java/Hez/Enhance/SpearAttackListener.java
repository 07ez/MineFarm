package Hez.Enhance;

import Hez.MineFarm.Main;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class SpearAttackListener implements Listener {
    // 엔티티별 활성 출혈 태스크 관리 (중복 실행 방지)
    private static final Map<UUID, BukkitTask> activeBleedTasks = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpearAttack(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        Player attacker = null;
        ItemStack spearItem = null;

        // 1. 공격자가 플레이어 직접 찌르기인지 확인
        if (event.getDamager() instanceof Player player) {
            attacker = player;
            spearItem = player.getInventory().getItemInMainHand();
        }
        // 2. 던져진 삼지창 투척 공격인지 확인
        else if (event.getDamager() instanceof Trident trident && trident.getShooter() instanceof Player player) {
            attacker = player;
            spearItem = trident.getItem();
        }

        if (attacker == null || spearItem == null) return;
        if (EnhanceEquipmentType.fromItem(spearItem) != EnhanceEquipmentType.SPEAR) return;

        int level = EnhanceManager.getEnhanceLevel(spearItem);
        if (level <= 0) return;

        SpearBleedConfig config = EnhanceManager.getSpearConfig(level);
        if (config == null) return;

        // [효과 1] 방어력 관통 (Armor Penetration)
        applyArmorPenetration(event, target, config.getArmorPenetrationRate());

        // 타격 사운드 및 이펙트
        attacker.playSound(target.getLocation(), Sound.ITEM_TRIDENT_HIT, 1.0f, 1.2f);
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1.0, 0), 10, 0.3, 0.3, 0.3, 0.1);

        // [효과 2] 출혈 (Bleeding) 부여
        if (config.getBleedChance() > 0) {
            double roll = ThreadLocalRandom.current().nextDouble() * 100.0;
            if (roll < config.getBleedChance()) {
                applyBleeding(attacker, target, config, level);
            }
        }
    }

    /**
     * 마인크래프트 바닐라 방어력 경감 수식을 기반으로 관통율(0.1 ~ 1.0)만큼 피해 보정
     */
    private void applyArmorPenetration(EntityDamageByEntityEvent event, LivingEntity target, double penetrationRate) {
        AttributeInstance armorAttr = target.getAttribute(Attribute.ARMOR);
        AttributeInstance toughnessAttr = target.getAttribute(Attribute.ARMOR_TOUGHNESS);

        double armor = (armorAttr != null) ? armorAttr.getValue() : 0.0;
        double toughness = (toughnessAttr != null) ? toughnessAttr.getValue() : 0.0;

        if (armor <= 0) return; // 방어력이 없는 대상은 그대로 진행

        double originalDamage = event.getDamage();

        // 바닐라 방어력 경감 비율 공식: min(20, max(armor / 5, armor - 4 * damage / (toughness + 8))) / 25
        double reductionFactor = Math.min(20.0, Math.max(armor / 5.0, armor - (4.0 * originalDamage) / (toughness + 8.0))) / 25.0;
        reductionFactor = Math.max(0.0, Math.min(0.80, reductionFactor)); // 최대 80% 경감

        // 관통율만큼 경감율을 무시하여 최종 데미지 상향
        // 최종 실피해 = 원래 피해 * (1 - (경감율 * (1 - 관통율)))
        double effectiveReduction = reductionFactor * (1.0 - penetrationRate);
        double finalDamage = originalDamage * (1.0 - effectiveReduction);

        event.setDamage(finalDamage);
    }

    /**
     * 출혈 디버프 및 둔화 효과 적용
     */
    private void applyBleeding(Player attacker, LivingEntity target, SpearBleedConfig config, int level) {
        // 기존 진행 중인 출혈이 있다면 취소하고 새로 갱신
        BukkitTask existing = activeBleedTasks.remove(target.getUniqueId());
        if (existing != null) {
            existing.cancel();
        }

        // 둔화 효과 부여
        if (config.getSlownessAmplifier() >= 0) {
            int durationTicks = (int) (config.getBleedDurationSeconds() * 20);
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, config.getSlownessAmplifier(), false, false, true));
        }

        attacker.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.4f);
        attacker.sendActionBar(net.kyori.adventure.text.Component.text(
                "§c[출혈 발동] §f적에게 §4출혈 §f효과를 부여했습니다! §7(+" + level + "강)"
        ));

        // 0.5초(10틱)마다 출혈 데미지 주기
        int totalTicks = (int) (config.getBleedDurationSeconds() * 2);
        double damagePerTick = config.getBleedDamagePerTick();

        BukkitTask task = new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (!target.isValid() || target.isDead() || count >= totalTicks) {
                    activeBleedTasks.remove(target.getUniqueId());
                    cancel();
                    return;
                }

                // 고정 출혈 피해 (방어력 무시 트루 데미지)
                target.damage(damagePerTick);

                // 출혈 피 파티클 효과
                target.getWorld().spawnParticle(
                        Particle.DUST,
                        target.getLocation().add(0, 1.0, 0),
                        12,
                        0.3, 0.4, 0.3,
                        new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.2f)
                );
                target.getWorld().playSound(target.getLocation(), Sound.BLOCK_POINTED_DRIPSTONE_DRIP_LAVA, 0.7f, 1.5f);

                count++;
            }
        }.runTaskTimer(Main.getInstance(), 10L, 10L);

        activeBleedTasks.put(target.getUniqueId(), task);
    }
}
