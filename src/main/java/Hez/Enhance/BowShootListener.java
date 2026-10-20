package Hez.Enhance;

import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class BowShootListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack bow = event.getBow();
        if (EnhanceEquipmentType.fromItem(bow) != EnhanceEquipmentType.BOW) return;

        int level = EnhanceManager.getEnhanceLevel(bow);
        if (level <= 0) return;

        BowRapidConfig config = EnhanceManager.getBowConfig(level);
        if (config == null) return;

        if (!(event.getProjectile() instanceof AbstractArrow arrow)) return;

        // [1] 속사 (차징 시간 단축 보정 및 탄속 가속)
        float originalForce = event.getForce();
        float boostedForce = (float) Math.min(1.0, originalForce * (1.0 + config.getChargeBonus()));

        // 살짝만 당겨도 풀차징 크리티컬 화살로 전환
        if (boostedForce >= 0.85f) {
            arrow.setCritical(true);
        }

        // 탄속 추가 가속
        Vector baseVelocity = arrow.getVelocity().multiply(1.0 + config.getProjectileSpeedBonus());
        arrow.setVelocity(baseVelocity);

        // 10강 관통 속성 적용
        if (config.getPierceLevel() > 0) {
            arrow.setPierceLevel(config.getPierceLevel());
        }

        // [2] 멀티샷 (다발 사격) - 6강 이상 (3~5발 부채꼴 발사)
        if (config.isMultiShot()) {
            int count = config.getArrowCount();
            List<Double> angles = calculateSpreadAngles(count);

            for (double angleDeg : angles) {
                Vector rotated = rotateVectorY(baseVelocity, angleDeg);

                Arrow extra = player.getWorld().spawn(arrow.getLocation(), Arrow.class, spawned -> {
                    spawned.setShooter(player);
                    spawned.setVelocity(rotated);
                    spawned.setWeapon(bow);
                    spawned.setCritical(arrow.isCritical());
                    spawned.setFireTicks(arrow.getFireTicks());
                    spawned.setDamage(arrow.getDamage());
                    if (config.getPierceLevel() > 0) {
                        spawned.setPierceLevel(config.getPierceLevel());
                    }
                    // 추가 화살은 플레이어가 주울 수 없도록 설정 (아이템 복사 방지)
                    spawned.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                });
            }

            player.playSound(player.getLocation(), Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 1.3f);
            player.sendActionBar(Component.text(
                    "§b[멀티샷] §f화살 §e" + count + "발 §f동시 사격! §7(+" + level + "강)"
            ));
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.4f);
        }
    }

    /**
     * 화살 수에 따른 좌우 분산 각도 목록 계산 (도 단위)
     */
    private List<Double> calculateSpreadAngles(int arrowCount) {
        List<Double> angles = new ArrayList<>();
        switch (arrowCount) {
            case 3 -> {
                // 좌 8도, 우 8도 (중앙 1발은 이미 발사됨)
                angles.add(-8.0);
                angles.add(8.0);
            }
            case 4 -> {
                // 좌 10도, 좌 4도, 우 6도 (총 4발)
                angles.add(-10.0);
                angles.add(-4.0);
                angles.add(6.0);
            }
            case 5 -> {
                // 좌 14도, 좌 7도, 우 7도, 우 14도 (총 5발)
                angles.add(-14.0);
                angles.add(-7.0);
                angles.add(7.0);
                angles.add(14.0);
            }
            default -> {}
        }
        return angles;
    }

    /**
     * 벡터를 수평(Y축) 기준으로 주어진 각도(도)만큼 회전
     */
    private Vector rotateVectorY(Vector v, double angleDegrees) {
        double rad = Math.toRadians(angleDegrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        double x = v.getX() * cos - v.getZ() * sin;
        double z = v.getX() * sin + v.getZ() * cos;

        return new Vector(x, v.getY(), z);
    }
}
