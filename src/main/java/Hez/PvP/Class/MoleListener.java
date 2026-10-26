package Hez.PvP.Class;

import Hez.PvP.ColosseumManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 두더지(Mole) 직업 스킬 리스너:
 * - 호퍼('땅굴로 들어가기')를 든 상태에서 F키(스왑 핸드) 사용 시 토글
 *   (호퍼가 왼손에 남지 않고 항상 메인 핫바에 그대로 유지됨)
 * - ON: 어둠 100 + 실명 100 + 투명화 부여, 플레이어 크기 0.5배 축소, 무적 및 공격 불가, 발 쪽 블록 스텝 파티클 2배 연출
 * - 이동 시: 걸어다닐 때만 발 아래 블록 스텝 파티클 지속 생성
 * - OFF: 효과 클리어, 크기 1.0배 복구, 무적 해제, 발 쪽 파티클 2배 연출
 */
public class MoleListener implements Listener {

    private final ColosseumManager colosseumManager;
    private static final Set<UUID> burrowedMoles = ConcurrentHashMap.newKeySet();

    public MoleListener(ColosseumManager colosseumManager) {
        this.colosseumManager = colosseumManager;
    }

    public static boolean isBurrowed(UUID uuid) {
        return burrowedMoles.contains(uuid);
    }

    public static Set<UUID> getBurrowedMoles() {
        return burrowedMoles;
    }

    public static void enterBurrow(Player player) {
        if (player == null || !player.isOnline()) return;
        burrowedMoles.add(player.getUniqueId());

        // 1. 어둠 100 + 실명 100 + 투명화 (지속시간 무제한)
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, PotionEffect.INFINITE_DURATION, 99, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, PotionEffect.INFINITE_DURATION, 99, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, false, false, true));

        // 2. 플레이어 크기 0.5배(절반) 축소
        AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(0.5);
        }

        // 3. 진입 시 발 쪽 블록 파티클 2배
        spawnBurrowParticles(player, 40);

        // 4. 사운드 및 안내 메시지
        try {
            player.playSound(player.getLocation(), Sound.BLOCK_GRAVEL_BREAK, 1.0f, 0.8f);
        } catch (Exception ignored) {}
        player.sendMessage("§6[두더지] §e땅굴 속으로 들어갔습니다! (무적 및 공격 불가, 크기 축소)");
    }

    public static void exitBurrow(Player player) {
        if (player == null || !player.isOnline()) return;
        burrowedMoles.remove(player.getUniqueId());

        // 1. 효과 클리어
        player.removePotionEffect(PotionEffectType.DARKNESS);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);

        // 2. 플레이어 크기 1.0배(원상복구)
        AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(1.0);
        }

        // 3. 탈출 시 발 쪽 블록 파티클 2배
        spawnBurrowParticles(player, 40);

        // 4. 사운드 및 안내 메시지
        try {
            player.playSound(player.getLocation(), Sound.BLOCK_GRAVEL_BREAK, 1.0f, 1.2f);
        } catch (Exception ignored) {}
        player.sendMessage("§6[두더지] §a땅굴에서 나왔습니다!");
    }

    /**
     * 게임 종료 또는 리셋 시 모든 두더지의 땅굴 상태 및 스케일 초기화
     */
    public static void resetAll() {
        for (UUID uuid : burrowedMoles) {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.removePotionEffect(PotionEffectType.DARKNESS);
                p.removePotionEffect(PotionEffectType.BLINDNESS);
                p.removePotionEffect(PotionEffectType.INVISIBILITY);
                AttributeInstance scaleAttr = p.getAttribute(Attribute.SCALE);
                if (scaleAttr != null) {
                    scaleAttr.setBaseValue(1.0);
                }
            }
        }
        burrowedMoles.clear();
    }

    /**
     * 발 아래 블록 스텝 파티클 연출
     */
    private static void spawnBurrowParticles(Player player, int count) {
        Location loc = player.getLocation();
        Block blockUnder = loc.clone().subtract(0, 0.2, 0).getBlock();
        if (blockUnder.getType().isAir()) {
            blockUnder = loc.clone().subtract(0, 1.0, 0).getBlock();
        }
        BlockData blockData = blockUnder.getType().isAir() ? Material.DIRT.createBlockData() : blockUnder.getBlockData();
        player.getWorld().spawnParticle(Particle.BLOCK, loc.clone().add(0, 0.1, 0), count, 0.4, 0.1, 0.4, 0.15, blockData);
    }

    private boolean isHopperSkillItem(ItemStack item) {
        if (item == null || item.getType() != Material.HOPPER) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() && meta.getDisplayName().contains("땅굴로 들어가기");
    }

    /**
     * 호퍼('땅굴로 들어가기')를 든 상태에서 F키(스왑 핸드) 입력 감지:
     * - 왼손으로 넘어가지 않고 무조건 메인 핫바에 그대로 유지!
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();

        // 1. 경기 진행 중인지 확인
        if (colosseumManager == null || !colosseumManager.isMatchInProgress()) {
            return;
        }

        // 2. 두더지 직업인지 확인
        Map<UUID, String> selectedClasses = colosseumManager.getSelectedClasses();
        if (selectedClasses == null || !"두더지".equals(selectedClasses.get(player.getUniqueId()))) {
            return;
        }

        // 3. 메인 손(또는 교체 대상 아이템)이 호퍼('땅굴로 들어가기')인지 확인
        ItemStack currentMain = player.getInventory().getItemInMainHand();
        ItemStack offGoingItem = event.getOffHandItem(); // 메인손에서 왼손으로 가려던 아이템
        ItemStack currentOff = player.getInventory().getItemInOffHand();

        ItemStack hopperItem = null;
        if (isHopperSkillItem(currentMain)) {
            hopperItem = currentMain;
        } else if (isHopperSkillItem(offGoingItem)) {
            hopperItem = offGoingItem;
        } else if (isHopperSkillItem(currentOff)) {
            hopperItem = currentOff;
        }

        if (hopperItem == null) {
            return;
        }

        // F키 교체 이벤트 취소 -> 왼손과 오른손 아이템 교체 방지 (기존 왼손 아이템 완벽 보존)
        event.setCancelled(true);
        player.updateInventory();

        // 토글 동작 (ON <-> OFF)
        if (isBurrowed(player.getUniqueId())) {
            exitBurrow(player);
        } else {
            enterBurrow(player);
        }
    }

    /**
     * 땅굴 모드일 때 플레이어가 걸어다닐 때만 발 아래 블록 스텝 파티클 연출
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isBurrowed(player.getUniqueId())) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // 실제 좌표 이동이 있을 때만 (시선 회전만 할 때는 파티클 미생성)
        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) {
            return;
        }

        Location loc = player.getLocation();
        Block blockUnder = loc.clone().subtract(0, 0.2, 0).getBlock();
        if (blockUnder.getType().isAir()) {
            blockUnder = loc.clone().subtract(0, 0.8, 0).getBlock();
        }
        BlockData blockData = blockUnder.getType().isAir() ? Material.DIRT.createBlockData() : blockUnder.getBlockData();

        // 걸을 때 튀어나오는 땅 파티클 연출
        player.getWorld().spawnParticle(Particle.BLOCK, loc.clone().add(0, 0.05, 0), 4, 0.15, 0.05, 0.15, 0.05, blockData);
    }

    /**
     * 땅굴 상태에서는 나도 상대를 못 때리고, 상대도 나를 못 때림 (무적 & 공격 불가)
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // 피해자가 땅굴 상태인 경우 피격 무효화
        if (event.getEntity() instanceof Player victim && isBurrowed(victim.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // 공격자가 땅굴 상태인 경우 공격 무효화
        if (event.getDamager() instanceof Player attacker && isBurrowed(attacker.getUniqueId())) {
            event.setCancelled(true);
            attacker.sendMessage("§c[두더지] 땅굴 상태에서는 공격할 수 없습니다!");
            return;
        }

        // 공격 투사체를 발사한 자가 땅굴 상태인 경우 공격 무효화
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            if (isBurrowed(shooter.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * 땅굴 상태에서는 모든 환경 피해(낙하, 질식 등)도 무효화
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player victim && isBurrowed(victim.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * 접속 종료 시 두더지 땅굴 상태 해제
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (isBurrowed(player.getUniqueId())) {
            exitBurrow(player);
        }
    }
}
