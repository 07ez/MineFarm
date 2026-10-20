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
    private static final Map<UUID, Integer> flyingMinions = new ConcurrentHashMap<>();
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
        int flying = flyingMinions.getOrDefault(uuid, 0);

        // 유효하지 않은 디스플레이 정리
        current.removeIf(d -> d == null || !d.isValid());

        // 처음 삼지창을 장착했거나 리셋된 경우 targetCount만큼 생성
        if (current.isEmpty() && flying == 0) {
            current = createMinions(player, targetCount);
            playerMinions.put(uuid, current);
        } else if (current.size() + flying < targetCount) {
            // 강화 단계 상승 등으로 미니언이 부족한 경우 차이만큼 보충
            int diff = targetCount - (current.size() + flying);
            for (int i = 0; i < diff; i++) {
                current.add(spawnSingleMinion(player));
            }
        } else if (current.size() + flying > targetCount && !current.isEmpty()) {
            // 강화 단계 하락 등으로 초과된 경우 초과분 제거
            while (current.size() + flying > targetCount && !current.isEmpty()) {
                ItemDisplay last = current.remove(current.size() - 1);
                if (last != null && last.isValid()) last.remove();
            }
        }

        // 우클릭 차징 중인지 확인 (손을 올리고 투척 차징 상태인지)
        boolean isCharging = player.isHandRaised();
        updateMinionPositions(player, current, isCharging);
    }

    public static ItemDisplay spawnSingleMinion(Player player) {
        return player.getWorld().spawn(player.getEyeLocation(), ItemDisplay.class, d -> {
            d.setItemStack(new ItemStack(Material.TRIDENT));
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.THIRDPERSON_RIGHTHAND);
            d.setTransformation(IDLE_TRANSFORMATION);
            d.setInterpolationDuration(2);
            d.setInterpolationDelay(0);
            d.setViewRange(64.0f);
        });
    }

    private static List<ItemDisplay> createMinions(Player player, int count) {
        List<ItemDisplay> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(spawnSingleMinion(player));
        }
        return list;
    }

    /**
     * 충성 복귀한 삼지창이 플레이어에게 도달했을 때 주운 순서대로 등 뒤에 1자루씩 복귀!
     */
    public static void onMinionReturned(Player player) {
        if (player == null || !player.isOnline()) return;
        UUID uuid = player.getUniqueId();

        int flying = flyingMinions.getOrDefault(uuid, 0);
        if (flying > 0) {
            flyingMinions.put(uuid, flying - 1);
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (EnhanceEquipmentType.fromItem(held) != EnhanceEquipmentType.TRIDENT) return;

        int level = EnhanceManager.getEnhanceLevel(held);
        if (level <= 0) return;
        TridentMinionConfig config = EnhanceManager.getTridentConfig(level);
        if (config == null) return;

        List<ItemDisplay> current = playerMinions.computeIfAbsent(uuid, k -> new ArrayList<>());
        if (current.size() < config.getMinionCount()) {
            ItemDisplay minion = spawnSingleMinion(player);
            current.add(minion);

            player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RETURN, 0.6f, 1.25f + (current.size() * 0.05f));
            player.getWorld().spawnParticle(org.bukkit.Particle.ENCHANTED_HIT, minion.getLocation(), 10, 0.15, 0.15, 0.15, 0.05);
            player.sendActionBar(Component.text("§b[삼지창 미니언] §f복귀 완료 §e(" + current.size() + "/" + config.getMinionCount() + ")"));
        }

        // [안전망] 충성으로 인벤토리에 혹시 유입된 0강 일반 바닐라 삼지창이 있다면 즉시 제거
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack != null && stack.getType() == Material.TRIDENT) {
                // 손에 든 현재 슬롯이 아니고, 강화가 안 되어있고 이름도 없는 바닐라 삼지창이면 복제 유입물이므로 제거
                if (slot != player.getInventory().getHeldItemSlot() && EnhanceManager.getEnhanceLevel(stack) <= 0) {
                    if (!stack.hasItemMeta() || !stack.getItemMeta().hasDisplayName()) {
                        player.getInventory().setItem(slot, null);
                    }
                }
            }
        }
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
            player.sendActionBar(Component.text("§c[삼지창 미니언] 아직 복귀한 미니언이 없습니다!"));
            return false;
        }

        firingCooldowns.put(uuid, now);
        isFiring.add(uuid);

        List<ItemDisplay> toShoot = new ArrayList<>(current);
        current.clear(); // 등 뒤에서 모두 사출 처리

        // 날아가서 복귀해야 할 개수 기록
        flyingMinions.put(uuid, flyingMinions.getOrDefault(uuid, 0) + toShoot.size());

        player.sendActionBar(Component.text("§b[삼지창 미니언] §e" + toShoot.size() + "자루 §f일제 사출!"));

        // 1틱(0.05초) 시차를 두고 순차 사출 (머신건 연사 손맛 연출)
        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                if (index >= toShoot.size() || !player.isOnline()) {
                    isFiring.remove(uuid);
                    toShoot.forEach(ItemDisplay::remove);
                    cancel();
                    return;
                }

                ItemDisplay display = toShoot.get(index);
                Location eye = player.getEyeLocation();

                // [실시간 에임 추적] 발사 순간의 최신 시선 방향과 타겟 포인트를 실시간으로 계산!
                Vector currentAimDir = eye.getDirection().normalize();
                org.bukkit.util.RayTraceResult hit = player.getWorld().rayTrace(
                        eye, currentAimDir, 50.0,
                        org.bukkit.FluidCollisionMode.NEVER, true, 0.5,
                        e -> !e.equals(player) && !(e instanceof ItemDisplay) && !e.getScoreboardTags().contains("minion_trident")
                );

                Location currentTargetPoint = (hit != null && hit.getHitPosition() != null)
                        ? hit.getHitPosition().toLocation(player.getWorld())
                        : eye.clone().add(currentAimDir.clone().multiply(40.0));

                // [달리면서 던질 때 몸통 관통/자해 방지]
                // 등 뒤에서 앞으로 뚫고 나가지 않도록, 플레이어 시선 전방 1.2m 안전 위치에서 사출!
                Location spawnLoc = eye.clone().add(currentAimDir.clone().multiply(1.2));
                display.remove(); // 디스플레이 삼지창 삭제

                // 스폰 위치에서 그 순간의 플레이어 에임 목표점(currentTargetPoint)을 향해 정확히 사출!
                Vector toTarget = currentTargetPoint.toVector().subtract(spawnLoc.toVector()).normalize();

                Trident fired = player.getWorld().spawn(spawnLoc, Trident.class, t -> {
                    t.addScoreboardTag("minion_trident"); // 미니언 삼지창 태그 부여 (리스너 취소 방지!)
                    t.setShooter(player);
                    t.setVelocity(toTarget.multiply(2.8)); // 그 순간 조준점에 정확히 꽂힘!
                    t.setDamage(config.getDamagePerMinion());
                    t.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                    t.setLoyaltyLevel(3); // [충성 3레벨 적용: 땅에 박히면 플레이어에게 날아오는 애니메이션!]
                    t.setCritical(true);
                    // [중요: 내부 아이템을 AIR로 설정하여 충성 복귀 시 인벤토리 침투를 원천 차단!]
                    try {
                        t.setItemStack(new ItemStack(Material.AIR));
                        t.setItem(new ItemStack(Material.AIR));
                    } catch (Throwable ignored) {}
                });

                // 충성 복귀 추적: 날아갔다가 플레이어에게 복귀하면 인벤토리 들어가지 않게 즉시 제거 & 등 뒤로 1자루 복귀!
                new BukkitRunnable() {
                    int ticks = 0;
                    boolean processed = false;
                    boolean hasLeftPlayer = false; // 발사 후 플레이어 품을 벗어났는지 여부

                    @Override
                    public void run() {
                        ticks++;
                        if (processed) {
                            cancel();
                            return;
                        }

                        // 8초(160틱) 경과하거나 유효하지 않으면 안전 복귀 처리
                        if (ticks > 160 || !fired.isValid() || !player.isOnline()) {
                            if (fired.isValid()) fired.remove();
                            processed = true;
                            onMinionReturned(player);
                            cancel();
                            return;
                        }

                        double distSq = fired.getLocation().distanceSquared(player.getLocation().add(0, 1, 0));

                        // 발사 후 3m 이상 멀어지면 벗어남 플래그 true
                        if (distSq > 9.0) {
                            hasLeftPlayer = true;
                        }

                        // 복귀 판정: 블록에 박혔거나 피해를 입힌 후, 또는 멀어졌다가 플레이어 반경 3.8m(distSq <= 14.44) 내로 복귀했을 때!
                        boolean isReturning = fired.isInBlock() || fired.hasDealtDamage() || hasLeftPlayer || ticks > 15;
                        if (isReturning && distSq <= 14.44) {
                            processed = true;
                            fired.remove(); // 바닐라 playerTouch(0.8m)에 닿기 전 공중 분해 (인벤토리 복제 100% 차단)
                            onMinionReturned(player); // 등 뒤에 1자루 복귀 소환!
                            cancel();
                        }
                    }
                }.runTaskTimer(Main.getInstance(), 1L, 1L);

                player.getWorld().playSound(spawnLoc, Sound.ITEM_TRIDENT_THROW, 1.0f, 1.2f + (index * 0.05f));
                index++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 2L);

        return true;
    }

    public static void clearMinions(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        flyingMinions.remove(uuid);
        List<ItemDisplay> list = playerMinions.remove(uuid);
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
        flyingMinions.clear();
        firingCooldowns.clear();
        isFiring.clear();
    }
}
