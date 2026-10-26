package Hez.PvP.Class;

import Hez.PvP.ColosseumManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;

/**
 * 텔레포터(Teleporter) 직업 스킬 리스너:
 * - 메인 손에 청금석을 들고 F키(양손 스왑) 입력 시 발동
 * - 바라보는 시선 방향으로 최대 8블록 순간이동 (벽 충돌 방지 및 안전 위치 착지)
 * - 배고픔 7 소모 (7 미만 시 발동 불가)
 */
public class TeleporterListener implements Listener {

    private final ColosseumManager colosseumManager;

    public TeleporterListener(ColosseumManager colosseumManager) {
        this.colosseumManager = colosseumManager;
    }

    private boolean isTeleporter(Player player) {
        if (colosseumManager == null || !colosseumManager.isMatchInProgress()) {
            return false;
        }
        Map<UUID, String> selectedClasses = colosseumManager.getSelectedClasses();
        return selectedClasses != null && "텔레포터".equals(selectedClasses.get(player.getUniqueId()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!isTeleporter(player)) return;

        // 메인 손에서 오프 손으로 넘어가려는 아이템 확인 (getOffHandItem()이 원래 메인 손에 있던 아이템)
        ItemStack mainItem = event.getOffHandItem();
        if (mainItem == null || mainItem.getType() != Material.LAPIS_LAZULI) return;

        // 청금석이 왼손으로 넘어가지 않도록 이벤트 취소 및 기존 왼손 아이템 보존
        event.setCancelled(true);
        player.updateInventory();

        // 텔레포트 실행
        teleportForward(player);
    }

    private void teleportForward(Player player) {
        // 배고픔 게이지 검사 (최소 7 필요)
        int currentFood = player.getFoodLevel();
        if (currentFood < 7) {
            player.sendActionBar(Component.text("§c배고픔이 부족하여 순간이동할 수 없습니다! (필요: 7)"));
            try {
                player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1.0f, 1.2f);
            } catch (Exception ignored) {}
            return;
        }

        // 배고픔 7 차감
        player.setFoodLevel(currentFood - 7);

        Location origin = player.getLocation();
        Vector dir = origin.getDirection().normalize();
        World world = player.getWorld();

        double maxDist = 8.0;
        double step = 0.5;
        double finalDist = 0.0;

        // 0.5블록 단위로 전진하며 벽(고체 블록) 검사
        for (double d = 0.5; d <= maxDist; d += step) {
            Location checkLoc = origin.clone().add(dir.clone().multiply(d));
            Block feetBlock = checkLoc.getBlock();
            Block headBlock = checkLoc.clone().add(0, 1.0, 0).getBlock();

            // 발 위치나 머리 위치가 고체 블록(벽)이면 그 직전까지만 전진
            if (feetBlock.getType().isSolid() || headBlock.getType().isSolid()) {
                break;
            }
            finalDist = d;
        }

        Location targetLoc;
        if (finalDist > 0.0) {
            targetLoc = origin.clone().add(dir.clone().multiply(finalDist));
        } else {
            targetLoc = origin.clone();
        }

        // 원래 플레이어의 시선 각도(Yaw, Pitch) 유지
        targetLoc.setYaw(origin.getYaw());
        targetLoc.setPitch(origin.getPitch());

        // 출발지 파티클 및 사운드
        try {
            world.spawnParticle(Particle.PORTAL, origin.clone().add(0, 1.0, 0), 25, 0.3, 0.5, 0.3, 0.1);
            world.playSound(origin, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
        } catch (Exception ignored) {}

        // 텔레포트
        player.teleport(targetLoc);

        // 도착지 파티클 및 사운드
        try {
            world.spawnParticle(Particle.PORTAL, targetLoc.clone().add(0, 1.0, 0), 25, 0.3, 0.5, 0.3, 0.1);
            world.playSound(targetLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
        } catch (Exception ignored) {}
    }
}
