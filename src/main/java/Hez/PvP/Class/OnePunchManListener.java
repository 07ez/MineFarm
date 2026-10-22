package Hez.PvP.Class;

import Hez.PvP.ColosseumManager;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class OnePunchManListener implements Listener {

    private final ColosseumManager manager;

    // 허공 스윙 연타로 인한 급사 방지용 최소 쿨타임 (350ms)
    private final Map<UUID, Long> lastAirSwingTime = new ConcurrentHashMap<>();

    public OnePunchManListener(ColosseumManager manager) {
        this.manager = manager;
    }

    /**
     * 원펀맨 허공 스윙(공기 좌클릭) 시 7 피해 적용
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onAirSwing(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR) {
            return;
        }

        Player player = event.getPlayer();

        // 콜로세움 결투장 월드(colosseum)이고 실제 경기가 진행 중일 때만 발동 (대기실에서는 피해 없음)
        if (!manager.isArenaWorld(player.getWorld()) || !manager.isMatchInProgress()) {
            return;
        }

        // 선택한 직업이 원펀맨인지 확인
        String selectedClass = manager.getSelectedClass(player.getUniqueId());
        if (!"원펀맨".equals(selectedClass)) {
            return;
        }

        // 주 무기(메인핸드)가 맨손인지 확인
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand.getType() != Material.AIR) {
            return;
        }

        // 최소 쿨타임 350ms 체크 (패킷 중복 및 마우스 연타 시 비정상 급사 방지)
        long now = System.currentTimeMillis();
        Long lastTime = lastAirSwingTime.get(player.getUniqueId());
        if (lastTime != null && (now - lastTime) < 350) {
            return;
        }
        lastAirSwingTime.put(player.getUniqueId(), now);

        // 허공 스윙 자해 데미지 7.0 적용 (하트 3.5칸)
        player.damage(7.0);
        player.sendMessage("§c[원펀맨] §f허공에 주먹을 휘둘러 7의 피해를 입었습니다!");

        try {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.7f, 1.0f);
            player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, player.getLocation().add(0, 1.0, 0), 3, 0.2, 0.2, 0.2, 0.1);
        } catch (Exception ignored) {
        }
    }
}
