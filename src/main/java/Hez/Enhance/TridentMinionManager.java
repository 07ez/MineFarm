package Hez.Enhance;

import Hez.MineFarm.Main;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TridentMinionManager {
    private static final Map<UUID, List<ItemDisplay>> playerMinions = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> firingCooldowns = new ConcurrentHashMap<>();
    private static final Set<UUID> isFiring = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static BukkitTask updateTask = null;

    /**
     * 미니언 추적 및 위치 갱신 태스크 시작
     */
    public static void startTask() {
        if (updateTask != null) updateTask.cancel();

        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updatePlayerMinions(player);
                }
            }
        }.runTaskTimer(Main.getInstance(), 1L, 1L);
    }

    // 평상시(대기): 세워진 기본 각도
    private static final Transformation IDLE_TRANSFORMATION = new Transformation(
            new Vector3f(0, 0, 0),
            new Quaternionf().rotateXYZ(0f, 0f, 0f),
            new Vector3f(0.85f, 0.85f, 0.85f),
            new Quaternionf()
    );

    // 차징 시: 그 자리에서 동시에 X축 90도 회전! (정면 공격 조준)
    private static final Transformation CHARGE_TRANSFORMATION = new Transformation(
            new Vector3f(0, 0, 0),
            new Quaternionf().rotateXYZ((float) Math.toRadians(90.0), 0f, 0f),
            new Vector3f(0.85f, 0.85f, 0.85f),
            new Quaternionf()
    );

    /**
     * 플레이어 주변 미니언 상태 업데이트 (들고 있는 손 확인 및 위치 동기화)
     */
    public static void updatePlayerMinions(Player player) {
        UUID uuid = player.getUniqueId();
        ItemStack held = player.getInventory().getItemInMainHand();

        // 사출(발사) 연출 중에는 위치 갱신을 잠시 대기
        if (isFiring.contains(uuid)) return;

        // 삼지창을 들고 있지 않거나 강화 레벨이 없으면 제거
        if (EnhanceEquipmentType.fromItem(held) != EnhanceEquipmentType.TRIDENT) {
            clearMinions(player);
            return;
        }

        int level = EnhanceManager.getEnhanceLevel(held);
        if (level <= 0) {
            clearMinions(player);
            return;
        }

        TridentMinionConfig config = EnhanceManager.getTridentConfig(level);
        if (config == null) {
            clearMinions(player);
            return;
        }

        int targetCount = config.getMinionCount();
        List<ItemDisplay> current = playerMinions.computeIfAbsent(uuid, k -> new ArrayList<>());

        // 개수가 다르면 기존 것 정리 후 새로 생성
        if (current.size() != targetCount || current.stream().anyMatch(d -> !d.isValid())) {
            clearMinions(player);
            current = createMinions(player, targetCount);
            playerMinions.put(uuid, current);
        }

        // 우클릭 차징 중인지 확인 (손을 올리고 투척 차징 상태인지)
        boolean isCharging = player.isHandRaised();
        updateMinionPositions(player, current, isCharging);
    }

    private static List<ItemDisplay> createMinions(Player player, int count) {
        List<ItemDisplay> list = new ArrayList<>();
        Location eyeLoc = player.getEyeLocation();

        for (int i = 0; i < count; i++) {
            ItemDisplay display = player.getWorld().spawn(eyeLoc, ItemDisplay.class, d -> {
                d.setItemStack(new ItemStack(Material.TRIDENT));
                d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.THIRDPERSON_RIGHTHAND);
                d.setTransformation(IDLE_TRANSFORMATION);
                d.setInterpolationDuration(2);
                d.setInterpolationDelay(0);
                d.setViewRange(64.0f);
            });
            list.add(display);
        }
        return list;
    }

    /**
     * 플레이어 등 뒤 부채꼴 호버링 위치 갱신
     * isCharging이 true이면 그 자리에서 동시에 X축이 90도 회전하여 공격 조준 자세로 전환!
     */
    private static void updateMinionPositions(Player player, List<ItemDisplay> minions, boolean isCharging) {
        int n = minions.size();
        if (n == 0) return;

        Location eye = player.getEyeLocation();
        float yaw = eye.getYaw();
        float pitch = eye.getPitch();

        double spreadAngle = 140.0; // 등 뒤 부채꼴 총 각도
        double startAngle = -spreadAngle / 2.0;
        double step = (n > 1) ? spreadAngle / (n - 1) : 0;

        Transformation targetTransform = isCharging ? CHARGE_TRANSFORMATION : IDLE_TRANSFORMATION;

        for (int i = 0; i < n; i++) {
            ItemDisplay display = minions.get(i);
            if (!display.isValid()) continue;

            double angleOffset = (n == 1) ? 0 : (startAngle + i * step);
            // 플레이어 등 뒤 각도 (yaw + 180 + angleOffset)
            double rad = Math.toRadians(yaw + 180 + angleOffset);

            double radius = 1.35; // 플레이어 중심으로부터의 거리
            double x = eye.getX() + radius * -Math.sin(rad);
            double z = eye.getZ() + radius * Math.cos(rad);
            double y = eye.getY() - 0.2 + (Math.sin(Math.toRadians(i * 30)) * 0.15); // 약간의 곡선 높낮이

            Location targetLoc = new Location(player.getWorld(), x, y, z, yaw, pitch);
            display.teleport(targetLoc);

            // 그 자리에서 각도 변환 (차징 시 X축 90도)
            display.setTransformation(targetTransform);

            // 차징 중 미니언 위치에서 마력 파티클 방출 (공격 감각 극대화)
            if (isCharging) {
                player.getWorld().spawnParticle(org.bukkit.Particle.CRIT, targetLoc, 1, 0.05, 0.05, 0.05, 0.02);
            }
        }

        if (isCharging) {
            player.playSound(eye, Sound.ITEM_TRIDENT_RETURN, 0.2f, 1.8f);
        }
    }

    /**
     * 미니언 일제 사출 (우클릭 차징 후 발사 시)
     */
    public static boolean fireMinions(Player player, ItemStack tridentItem) {
        UUID uuid = player.getUniqueId();
        int level = EnhanceManager.getEnhanceLevel(tridentItem);
        if (level <= 0) return false;

        TridentMinionConfig config = EnhanceManager.getTridentConfig(level);
        if (config == null) return false;

        long now = System.currentTimeMillis();
        long lastFired = firingCooldowns.getOrDefault(uuid, 0L);
        long cdMillis = (long) (config.getCooldownSeconds() * 1000);

        if (now - lastFired < cdMillis) {
            double remaining = (cdMillis - (now - lastFired)) / 1000.0;
            player.sendActionBar(Component.text(String.format("§c[삼지창 미니언] 재장전 중! (%.1f초 남음)", remaining)));
            return false;
        }

        List<ItemDisplay> current = playerMinions.get(uuid);
        if (current == null || current.isEmpty()) {
            return false;
        }

        firingCooldowns.put(uuid, now);
        isFiring.add(uuid);

        List<ItemDisplay> toShoot = new ArrayList<>(current);
        current.clear(); // 소환된 디스플레이는 사출 처리

        player.sendActionBar(Component.text("§b[삼지창 미니언] §e" + toShoot.size() + "자루 §f일제 사출!"));

        // 플레이어가 조준하고 있는 정확한 에임 목표 지점(targetPoint) 계산
        Location eye = player.getEyeLocation();
        Vector aimDir = eye.getDirection().normalize();

        org.bukkit.util.RayTraceResult hit = player.getWorld().rayTrace(
                eye, aimDir, 50.0,
                org.bukkit.FluidCollisionMode.NEVER, true, 0.5,
                e -> !e.equals(player) && !(e instanceof ItemDisplay) && !e.getScoreboardTags().contains("minion_trident")
        );

        Location targetPoint = (hit != null && hit.getHitPosition() != null)
                ? hit.getHitPosition().toLocation(player.getWorld())
                : eye.clone().add(aimDir.clone().multiply(40.0));

        // 1틱(0.05초) 시차를 두고 순차 사출 (머신건 연사 손맛 연출)
        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                if (index >= toShoot.size() || !player.isOnline()) {
                    isFiring.remove(uuid);
                    // 사출 종료 후 남은 디스플레이 정리
                    toShoot.forEach(ItemDisplay::remove);
                    cancel();
                    return;
                }

                ItemDisplay display = toShoot.get(index);
                Location spawnLoc = display.isValid() ? display.getLocation() : eye;
                display.remove(); // 디스플레이 삼지창 삭제

                // 각 미니언의 스폰 위치에서 플레이어의 에임 목표점(targetPoint)을 향해 정확히 사출!
                Vector toTarget = targetPoint.toVector().subtract(spawnLoc.toVector()).normalize();

                Trident fired = player.getWorld().spawn(spawnLoc, Trident.class, t -> {
                    t.addScoreboardTag("minion_trident"); // 미니언 삼지창 태그 부여 (리스너 취소 방지!)
                    t.setShooter(player);
                    t.setVelocity(toTarget.multiply(2.8)); // 조준점에 정확히 꽂힘!
                    t.setDamage(config.getDamagePerMinion());
                    t.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                    t.setLoyaltyLevel(3); // [충성 3레벨 적용: 땅에 박히면 플레이어에게 날아오는 애니메이션!]
                    t.setCritical(true);
                });

                // 충성 복귀 추적: 날아갔다가 플레이어 1.6m 이내로 복귀하면 자연스럽게 흡수 소멸
                new BukkitRunnable() {
                    int ticks = 0;

                    @Override
                    public void run() {
                        ticks++;
                        if (ticks > 200 || !fired.isValid() || !player.isOnline()) {
                            if (fired.isValid()) fired.remove();
                            cancel();
                            return;
                        }

                        // 발사 직후 8틱 이후부터 플레이어 근접 검사 (플레이어에게 복귀 완료 시 회수)
                        if (ticks > 8 && fired.getLocation().distanceSquared(player.getLocation().add(0, 1, 0)) <= 2.56) {
                            player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RETURN, 0.6f, 1.3f);
                            player.getWorld().spawnParticle(org.bukkit.Particle.ENCHANTED_HIT, fired.getLocation(), 8, 0.2, 0.2, 0.2, 0.05);
                            fired.remove();
                            cancel();
                        }
                    }
                }.runTaskTimer(Main.getInstance(), 2L, 1L);

                player.getWorld().playSound(spawnLoc, Sound.ITEM_TRIDENT_THROW, 1.0f, 1.2f + (index * 0.05f));
                index++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 2L);

        return true;
    }

    public static void clearMinions(Player player) {
        if (player == null) return;
        List<ItemDisplay> list = playerMinions.remove(player.getUniqueId());
        if (list != null) {
            list.forEach(display -> {
                if (display != null && display.isValid()) {
                    display.remove();
                }
            });
        }
    }

    public static void clearAllMinions() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        for (List<ItemDisplay> list : playerMinions.values()) {
            if (list != null) {
                list.forEach(display -> {
                    if (display != null && display.isValid()) {
                        display.remove();
                    }
                });
            }
        }
        playerMinions.clear();
        firingCooldowns.clear();
        isFiring.clear();
    }
}
