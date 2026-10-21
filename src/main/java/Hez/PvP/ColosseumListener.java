package Hez.PvP;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class ColosseumListener implements Listener {

    private final ColosseumManager manager;

    public ColosseumListener(ColosseumManager manager) {
        this.manager = manager;
    }

    /**
     * /tp town, /tp colosseum, /tp wild 단축어 가로채기 지원
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage().trim();
        String[] parts = msg.split("\\s+");
        if (parts.length >= 2 && (parts[0].equalsIgnoreCase("/tp") || parts[0].equalsIgnoreCase("/teleport"))) {
            String sub = parts[1].toLowerCase();
            Player player = event.getPlayer();
            if (!player.isOp() && !player.hasPermission("minefarm.admin")) {
                return;
            }

            switch (sub) {
                case "town", "마을" -> {
                    event.setCancelled(true);
                    boolean success = Hez.Menu.WarpManager.teleport(player, "town");
                    if (success) {
                        player.sendMessage("§a[MineFarm] 마을로 이동했습니다.");
                    } else {
                        player.sendMessage("§c[MineFarm] 마을 월드 스폰으로 이동하지 못했습니다.");
                    }
                }
                case "colosseum", "arena", "pvp", "콜로세움" -> {
                    event.setCancelled(true);
                    manager.teleportToArena(player);
                }
                case "wild", "야생" -> {
                    event.setCancelled(true);
                    if (Hez.MineFarm.Main.getInstance() != null && Hez.MineFarm.Main.getInstance().getWildManager() != null) {
                        Hez.MineFarm.Main.getInstance().getWildManager().teleportToWild(player);
                    } else {
                        player.sendMessage("§c[MineFarm] 야생 시스템이 활성화되지 않았습니다.");
                    }
                }
            }
        }
    }

    /**
     * 콜로세움 내부에서 웅크리기(Shift) 시 준비 상태 토글
     * (직업과 팀이 모두 선택된 상태에서만 준비 완료 가능)
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) {
            Player player = event.getPlayer();
            if (manager.isInside(player.getUniqueId())) {
                manager.toggleReady(player);
            }
        }
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

        // 콜로세움 내부에 있는 경우 직업 및 팀 선택 검사
        if (toIn) {
            manager.checkClassSelection(player, to);
            manager.checkTeamSelection(player, to);
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

        // 콜로세움 내부로 텔레포트된 경우 직업 및 팀 검사
        if (toIn) {
            manager.checkClassSelection(player, to);
            manager.checkTeamSelection(player, to);
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
