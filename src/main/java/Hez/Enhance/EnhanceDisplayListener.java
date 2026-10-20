package Hez.Enhance;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EnhanceDisplayListener implements Listener {
    public static final String ENHANCE_TAG = "enhance";

    // 클릭 중복 방지 쿨다운 (0.2초)
    private static final Map<UUID, Long> clickCooldowns = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 200L;

    /**
     * 허공 / 블록 우클릭 시 플레이어 시선에 있는 enhance ItemDisplay 감지
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemDisplay display = getTargetEnhanceDisplay(player);
        if (display == null) return;

        // 디스플레이 상호작용 성공 시 블록 설치/사용 취소
        event.setCancelled(true);

        if (!checkCooldown(player.getUniqueId())) return;

        handleItemSwap(player, display);
    }

    /**
     * 엔티티 직접 우클릭 시 감지
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        if (event.getRightClicked() instanceof ItemDisplay display && display.getScoreboardTags().contains(ENHANCE_TAG)) {
            event.setCancelled(true);
            Player player = event.getPlayer();

            if (!checkCooldown(player.getUniqueId())) return;

            handleItemSwap(player, display);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        if (event.getRightClicked() instanceof ItemDisplay display && display.getScoreboardTags().contains(ENHANCE_TAG)) {
            event.setCancelled(true);
        }
    }

    /**
     * 손에 든 아이템과 강화대 디스플레이 아이템을 교환(거치)
     */
    private void handleItemSwap(Player player, ItemDisplay display) {
        ItemStack held = player.getInventory().getItemInMainHand();
        ItemStack currentOnDisplay = display.getItemStack();

        boolean hasHeld = held != null && held.getType() != Material.AIR;
        boolean hasOnDisplay = currentOnDisplay != null && currentOnDisplay.getType() != Material.AIR;

        // 1. 손에 든 아이템이 있을 때: 강화 가능한 아이템인지 확인 후 거치/교환
        if (hasHeld) {
            if (!EnhanceManager.isEnhanceable(held)) {
                player.sendMessage("§c강화할 수 있는 장비(검, 도끼, 곡괭이, 삼지창, 활, 철퇴)만 거치할 수 있습니다.");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
                return;
            }

            ItemStack toPlace = held.clone();
            toPlace.setAmount(1);

            // 손에 1개 이상 들고 있었을 경우의 처리
            if (held.getAmount() > 1) {
                held.setAmount(held.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            }

            // 기존에 거치되어 있던 아이템이 있다면 플레이어에게 지급
            if (hasOnDisplay) {
                if (player.getInventory().getItemInMainHand().getType() == Material.AIR) {
                    player.getInventory().setItemInMainHand(currentOnDisplay);
                } else {
                    player.getInventory().addItem(currentOnDisplay);
                }
            }

            // 디스플레이에 아이템 설정 및 지정된 각도(X 90, Y 0, Z 45)로 회전 적용
            display.setItemStack(toPlace);
            applyEnhanceTransformation(display);

            // 개인 디스플레이 즉시 갱신 (잔상 제거 및 텍스트 실시간 반영)
            if (Hez.MineFarm.Main.getInstance().getDisplayManager() != null) {
                Hez.MineFarm.Main.getInstance().getDisplayManager().refreshPlayerDisplay(player);
            }

            player.playSound(display.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1.0f, 1.1f);
            player.sendActionBar(Component.text("§a[강화대] §f장비를 거치했습니다: §e" + getItemDisplayName(toPlace)));
        }
        // 2. 손이 빈손인데 디스플레이에 올려진 아이템이 있을 때: 거치된 아이템 회수
        else if (hasOnDisplay) {
            player.getInventory().setItemInMainHand(currentOnDisplay.clone());
            display.setItemStack(new ItemStack(Material.AIR));

            // 개인 디스플레이 즉시 갱신 (잔상 제거 및 텍스트 실시간 반영)
            if (Hez.MineFarm.Main.getInstance().getDisplayManager() != null) {
                Hez.MineFarm.Main.getInstance().getDisplayManager().refreshPlayerDisplay(player);
            }

            player.playSound(display.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.2f);
            player.sendActionBar(Component.text("§e[강화대] §f거치된 장비를 회수했습니다: §7" + getItemDisplayName(currentOnDisplay)));
        }
    }

    /**
     * 장비 종류별 트랜스폼 적용:
     * - 삼지창: translation(-1, -0.5, 0.5), rotation(x: 90, z: 90)
     * - 창: z 회전 없이 rotation(x: 90, z: 0)
     * - 기타 장비: rotation(x: 90, z: 45)
     */
    public static void applyEnhanceTransformation(ItemDisplay display) {
        ItemStack item = display.getItemStack();
        EnhanceEquipmentType eqType = EnhanceEquipmentType.fromItem(item);

        Vector3f translation;
        Quaternionf rotation;

        if (eqType == EnhanceEquipmentType.TRIDENT) {
            // 삼지창 전용: x: -1, y: -0.5, z: 0.5 / rotation x: 90, z: 90
            translation = new Vector3f(-1.0f, -0.5f, 0.5f);
            rotation = new Quaternionf().rotateXYZ(
                    (float) Math.toRadians(90.0),
                    (float) Math.toRadians(0.0),
                    (float) Math.toRadians(90.0)
            );
        } else if (eqType == EnhanceEquipmentType.SPEAR) {
            // 창 전용: z 회전 없이 x: 90, z: 0
            translation = new Vector3f(0f, 0f, 0f);
            rotation = new Quaternionf().rotateXYZ(
                    (float) Math.toRadians(90.0),
                    (float) Math.toRadians(0.0),
                    (float) Math.toRadians(0.0)
            );
        } else {
            // 기타 일반 장비(검, 도끼, 곡괭이, 활, 철퇴 등): x: 90, z: 45
            translation = new Vector3f(0f, 0f, 0f);
            rotation = new Quaternionf().rotateXYZ(
                    (float) Math.toRadians(90.0),
                    (float) Math.toRadians(0.0),
                    (float) Math.toRadians(45.0)
            );
        }

        Transformation transformation = new Transformation(
                translation,
                rotation,
                new Vector3f(0.85f, 0.85f, 0.85f), // 알맞은 크기 배율
                new Quaternionf()
        );

        display.setTransformation(transformation);
        display.setInterpolationDuration(2);
        display.setInterpolationDelay(0);
    }

    /**
     * 시선 경로 상의 enhance 태그를 가진 ItemDisplay 탐색
     */
    public static ItemDisplay getTargetEnhanceDisplay(Player player) {
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection();

        // 1. RayTrace로 정면 디스플레이 탐색 (최대 4.5m)
        RayTraceResult result = player.getWorld().rayTraceEntities(
                eye, dir, 4.5, 0.4,
                entity -> entity instanceof ItemDisplay && entity.getScoreboardTags().contains(ENHANCE_TAG)
        );

        if (result != null && result.getHitEntity() instanceof ItemDisplay display) {
            return display;
        }

        // 2. 근거리(3.0m) 내 시선 벡터와 가장 일치하는 디스플레이 탐색 (보조 감지)
        ItemDisplay best = null;
        double bestDot = 0.85; // 약 30도 이내

        for (Entity e : player.getNearbyEntities(3.0, 3.0, 3.0)) {
            if (e instanceof ItemDisplay display && display.getScoreboardTags().contains(ENHANCE_TAG)) {
                Vector toTarget = display.getLocation().toVector().subtract(eye.toVector()).normalize();
                double dot = dir.dot(toTarget);
                if (dot > bestDot) {
                    bestDot = dot;
                    best = display;
                }
            }
        }

        return best;
    }

    private boolean checkCooldown(UUID uuid) {
        long now = System.currentTimeMillis();
        Long last = clickCooldowns.get(uuid);
        if (last != null && (now - last) < COOLDOWN_MS) {
            return false;
        }
        clickCooldowns.put(uuid, now);
        return true;
    }

    private String getItemDisplayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().name();
    }
}
