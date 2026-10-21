package Hez.PvP;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class ColosseumListener implements Listener {

    private final ColosseumManager manager;

    public ColosseumListener(ColosseumManager manager) {
        this.manager = manager;
    }

    /**
     * 플레이어 이동 시 콜로세움 경계 진입/이탈 감지
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // X, Z 블록 좌표가 변경되지 않은 경우(단순 시선 회전, Y축 수직 점프/낙하 등) 연산 생략
        if (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        boolean fromIn = manager.isInColosseum(from);
        boolean toIn = manager.isInColosseum(to);

        if (!fromIn && toIn) {
            manager.handlePlayerEnter(player);
        } else if (fromIn && !toIn) {
            manager.handlePlayerLeave(player);
        }

        // 콜로세움 내부에 있는 경우 직업 선택 발판 밟기 검사
        if (toIn) {
            manager.checkClassSelection(player, to);
        }
    }

    /**
     * 텔레포트로 인한 콜로세움 진입/이탈 감지
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        Player player = event.getPlayer();
        boolean fromIn = manager.isInColosseum(from);
        boolean toIn = manager.isInColosseum(to);

        if (fromIn && !toIn) {
            manager.handlePlayerLeave(player);
        } else if (!fromIn && toIn) {
            manager.handlePlayerEnter(player);
        }

        // 콜로세움 내부로 텔레포트된 경우 발판 검사
        if (toIn) {
            manager.checkClassSelection(player, to);
        }
    }

    /**
     * 콜로세움 내부에서 사망 시 퇴장 처리
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (manager.isInside(player.getUniqueId())) {
            manager.handlePlayerLeave(player);
        }
    }

    /**
     * 리스폰 위치가 콜로세움 내부일 경우 입장 처리
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location respawnLoc = event.getRespawnLocation();
        if (manager.isInColosseum(respawnLoc)) {
            manager.handlePlayerEnter(player);
        }
    }

    /**
     * 콜로세움 내부에서 접속 종료 시 퇴장 처리
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (manager.isInside(player.getUniqueId())) {
            manager.handlePlayerLeave(player);
        }
    }

    /**
     * 콜로세움 내부에서 강제 퇴장(킥) 시 퇴장 처리
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        if (manager.isInside(player.getUniqueId())) {
            manager.handlePlayerLeave(player);
        }
    }
}
