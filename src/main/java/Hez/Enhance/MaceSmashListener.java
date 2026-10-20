package Hez.Enhance;

import Hez.MineFarm.Main;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MaceSmashListener implements Listener {
    private static final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private static final Map<UUID, MaceSmashConfig> activeJumps = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();

        // 강화대(ItemDisplay)를 조준하고 우클릭할 때는 철퇴 도약 스킬 발동 방지!
        if (EnhanceDisplayListener.getTargetEnhanceDisplay(player) != null) return;

        ItemStack held = player.getInventory().getItemInMainHand();

        if (EnhanceEquipmentType.fromItem(held) != EnhanceEquipmentType.MACE) return;

        int level = EnhanceManager.getEnhanceLevel(held);
        if (level <= 0) return;

        MaceSmashConfig config = EnhanceManager.getMaceConfig(level);
        if (config == null) return;

        long now = System.currentTimeMillis();
        long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long cdMillis = (long) (config.getCooldownSeconds() * 1000);

        if (now - lastUsed < cdMillis) {
            double remaining = (cdMillis - (now - lastUsed)) / 1000.0;
            player.sendActionBar(Component.text(String.format("§c[도약 강타] 쿨타임 중입니다! (%.1f초 남음)", remaining)));
            return;
        }

        cooldowns.put(player.getUniqueId(), now);
        activeJumps.put(player.getUniqueId(), config);

        // 슈퍼 점프 도약 (전방 살짝 + 하늘 높이)
        Vector dir = player.getLocation().getDirection().setY(0).normalize().multiply(0.4);
        dir.setY(config.getJumpVelocity());
        player.setVelocity(dir);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.0f, 0.9f);
        player.getWorld().spawnParticle(Particle.GUST, player.getLocation(), 15, 0.5, 0.5, 0.5, 0.1);
        player.sendActionBar(Component.text("§6[도약 강타] §f하늘 높이 도약했습니다! 착지 시 §e지진 스매시§f 발동!"));

        // 착지 감시 태스크 (공중에 떴다가 지면에 닿는 순간 감지)
        new BukkitRunnable() {
            int ticks = 0;
            boolean leftGround = false;

            @Override
            public void run() {
                if (!player.isOnline() || !activeJumps.containsKey(player.getUniqueId()) || ticks > 100) {
                    activeJumps.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                ticks++;

                // 도약 직후 공중에 뜬 상태 확인
                if (!player.isOnGround()) {
                    leftGround = true;
                }

                // 공중에 떴다가 다시 지면에 착지했을 때 지진 스매시 발동!
                if (leftGround && player.isOnGround()) {
                    activeJumps.remove(player.getUniqueId());
                    triggerEarthquakeSmash(player, config, level);
                    cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 5L, 1L);
    }

    /**
     * 슈퍼 점프로 인한 낙하 데미지 무효화 & 착지 스매시 보장
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player player)) return;

        MaceSmashConfig config = activeJumps.remove(player.getUniqueId());
        if (config != null) {
            // 자체 낙하 피해 완전 면제
            event.setCancelled(true);
            int level = EnhanceManager.getEnhanceLevel(player.getInventory().getItemInMainHand());
            triggerEarthquakeSmash(player, config, level);
        }
    }

    /**
     * 공중에서 몬스터를 직접 메이스로 스매시 타격했을 때
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSmashHitEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        MaceSmashConfig config = activeJumps.remove(player.getUniqueId());
        if (config != null) {
            int level = EnhanceManager.getEnhanceLevel(player.getInventory().getItemInMainHand());
            triggerEarthquakeSmash(player, config, level);
        }
    }

    /**
     * 착지 지진 스매시 충격파 및 광역 에어본 발동
     */
    private void triggerEarthquakeSmash(Player player, MaceSmashConfig config, int level) {
        Location center = player.getLocation();
        double radius = config.getEarthquakeRadius();

        // 1. 시각 및 청각 연출
        center.getWorld().playSound(center, Sound.ITEM_MACE_SMASH_GROUND, 1.2f, 0.8f);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
        center.getWorld().spawnParticle(Particle.EXPLOSION, center.add(0, 0.2, 0), 3, 0.3, 0.1, 0.3, 0.05);

        // 원형 충격파 파티클 (지면 파편)
        for (double r = 1.0; r <= radius; r += 1.5) {
            int points = (int) (r * 10);
            for (int i = 0; i < points; i++) {
                double angle = 2 * Math.PI * i / points;
                double x = center.getX() + r * Math.cos(angle);
                double z = center.getZ() + r * Math.sin(angle);
                Location pLoc = new Location(center.getWorld(), x, center.getY(), z);
                center.getWorld().spawnParticle(
                        Particle.BLOCK,
                        pLoc,
                        2, 0.1, 0.2, 0.1,
                        Material.DIRT.createBlockData()
                );
            }
        }

        // 2. 주변 적 탐색 및 지진 데미지 + 에어본 적용
        int hitCount = 0;
        Collection<Entity> nearby = center.getWorld().getNearbyEntities(center, radius, 3.5, radius);

        for (Entity entity : nearby) {
            if (entity.equals(player)) continue;
            if (entity instanceof LivingEntity target && !target.isDead()) {
                hitCount++;

                // 지진 데미지
                target.damage(config.getEarthquakeDamage(), player);

                // 6강 이상 광역 에어본 (적을 공중으로 붕 띄움)
                if (config.hasAirborne()) {
                    Vector air = target.getVelocity();
                    air.setY(config.getAirbornePower());
                    // 밖으로 살짝 밀려나게
                    Vector push = target.getLocation().toVector().subtract(center.toVector()).setY(0);
                    if (push.lengthSquared() > 0) {
                        push.normalize().multiply(0.3);
                        air.add(push);
                    }
                    target.setVelocity(air);
                }
            }
        }

        String airText = config.hasAirborne() ? " §b& 광역 에어본" : "";
        player.sendActionBar(Component.text(String.format(
                "§6[지진 스매시] §f반경 §e%.1fm §f지진 강타%s! §7(총 §c%d마리 §7타격)",
                radius, airText, hitCount
        )));
    }
}
