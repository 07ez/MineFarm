package Hez.PvP;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
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
     * 결투장 평지 월드 내에서 플레이어가 설치한 블록 기록 및 외벽/프리셋 구역 보호
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();
        if (manager.isArenaWorld(loc.getWorld())) {
            ArenaTemplateManager templateMgr = manager.getTemplateManager();
            if (templateMgr != null) {
                // 관리자가 아니고 크리에이티브가 아닌 경우 아레나 경계 바깥 설치 금지
                if (!player.isOp() && player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                    if (templateMgr.hasBounds() && !templateMgr.isInArenaBounds(loc)) {
                        event.setCancelled(true);
                        player.sendMessage("§c[콜로세움] 결투장 경기장 바깥에는 블록을 설치할 수 없습니다!");
                        return;
                    }
                }
                templateMgr.recordPlayerBlockPlace(loc);
            }
        }
    }

    /**
     * 결투장 평지 월드 내에서 블록 파괴 감지 및 기록 (원상복구용)
     * 경기장 외벽/관중석 및 프리셋 구역 보호
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        org.bukkit.block.Block block = event.getBlock();
        Location loc = block.getLocation();

        if (manager.isArenaWorld(loc.getWorld())) {
            ArenaTemplateManager templateMgr = manager.getTemplateManager();
            if (templateMgr != null) {
                // 관리자가 아니고 크리에이티브가 아닌 경우
                if (!player.isOp() && player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                    // 아레나 경계 바깥(프리셋 구역) 및 관중석 파괴 금지
                    if (templateMgr.hasBounds() && !templateMgr.isInArenaBounds(loc)) {
                        event.setCancelled(true);
                        player.sendMessage("§c[콜로세움] 경기장 외벽 및 관중석 구역은 파괴할 수 없습니다!");
                        return;
                    }
                }
                // 경기장 내부에서 파괴된 블록 원래 데이터 기록
                templateMgr.recordPlayerBlockBreak(loc, block.getBlockData());
            }
        }
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
