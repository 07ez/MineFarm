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

        // 플레이어 등 뒤 부채꼴 위치로 이동 (에임 전방을 겨눔)
        updateMinionPositions(player, current);
    }

    private static List<ItemDisplay> createMinions(Player player, int count) {
        List<ItemDisplay> list = new ArrayList<>();
        Location eyeLoc = player.getEyeLocation();

        for (int i = 0; i < count; i++) {
            ItemDisplay display = player.getWorld().spawn(eyeLoc, ItemDisplay.class, d -> {
                d.setItemStack(new ItemStack(Material.TRIDENT));
                d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.THIRDPERSON_RIGHTHAND);
                d.setTransformation(new Transformation(
                        new Vector3f(0, 0, 0),
                        new AxisAngle4f(0, 0, 0, 1),
                        new Vector3f(0.85f, 0.85f, 0.85f), // 크기 조정
                        new AxisAngle4f(0, 0, 0, 1)
                ));
                d.setInterpolationDuration(1);
                d.setInterpolationDelay(0);
                d.setViewRange(64.0f);
            });
            list.add(display);
        }
        return list;
    }

    private static void updateMinionPositions(Player player, List<ItemDisplay> minions) {
        int n = minions.size();
        if (n == 0) return;

        Location eye = player.getEyeLocation();
        float yaw = eye.getYaw();
        float pitch = eye.getPitch();

        double spreadAngle = 140.0; // 등 뒤 부채꼴 총 각도
        double startAngle = -spreadAngle / 2.0;
        double step = (n > 1) ? spreadAngle / (n - 1) : 0;

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
                Location spawnLoc = display.isValid() ? display.getLocation() : player.getEyeLocation();
                display.remove(); // 디스플레이 삼지창 삭제

                // 실제 삼지창 발사체 생성 및 플레이어 에임 방향 사출
                Vector dir = player.getEyeLocation().getDirection().normalize();
                Trident fired = player.getWorld().spawn(spawnLoc, Trident.class, t -> {
                    t.setShooter(player);
                    t.setVelocity(dir.multiply(2.8));
                    t.setDamage(config.getDamagePerMinion());
                    t.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                });

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
