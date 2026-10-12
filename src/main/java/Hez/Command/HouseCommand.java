package Hez.Command;

import Hez.House.HouseData;
import Hez.House.HouseManager;
import Hez.MineFarm.Main;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class HouseCommand implements CommandExecutor {
    private final Main plugin;
    private final HouseManager houseManager;

    public HouseCommand(Main plugin) {
        this.plugin = plugin;
        this.houseManager = plugin.getHouseManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelpMessage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        // 1. 청크 소유자 확인: /house who
        if (sub.equals("who")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§c이 명령어는 인게임 플레이어만 사용할 수 있습니다.");
                return true;
            }

            HouseData house = houseManager.GetHouseData(player.getLocation());
            if (house == null) {
                player.sendMessage("§e현재 위치는 주인이 없는 빈 청크입니다.");
            } else if (HouseManager.SERVER_UUID.equals(house.getOwner())) {
                player.sendMessage("§6현재 위치는 [서버 공용 구역]입니다.");
            } else {
                OfflinePlayer ownerPlayer = Bukkit.getOfflinePlayer(house.getOwner());
                String ownerName = ownerPlayer.getName() != null ? ownerPlayer.getName() : house.getOwner().toString();
                player.sendMessage("§a현재 위치는 §f" + ownerName + "§a님의 영지입니다.");
            }
            return true;
        }

        // 2. 현재 청크 등록: /house claim
        if (sub.equals("claim")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§c이 명령어는 인게임 플레이어만 사용할 수 있습니다.");
                return true;
            }

            Chunk chunk = player.getChunk();
            if (!houseManager.isTownWorld(chunk.getWorld())) {
                player.sendMessage("§c청크는 마을 월드에서만 등록할 수 있습니다.");
                return true;
            }

            boolean success = houseManager.claimChunk(player.getUniqueId(), chunk);
            if (success) {
                player.sendMessage("§a현재 청크를 당신의 영지로 등록하였습니다! (청크: " + chunk.getX() + ", " + chunk.getZ() + ")");
            } else {
                player.sendMessage("§c이미 소유자가 있거나 등록할 수 없는 청크입니다.");
            }
            return true;
        }

        // 3. 현재 청크 등록 해제: /house unclaim
        if (sub.equals("unclaim")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§c이 명령어는 인게임 플레이어만 사용할 수 있습니다.");
                return true;
            }

            Chunk chunk = player.getChunk();
            boolean success = houseManager.unclaimChunk(player.getUniqueId(), chunk);
            if (success) {
                player.sendMessage("§e현재 청크의 영지 등록을 해제하였습니다.");
            } else {
                player.sendMessage("§c당신이 소유한 청크가 아닙니다.");
            }
            return true;
        }

        // 4. 멤버 관리: /house member <add|remove|list> <플레이어>
        if (sub.equals("member")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§c이 명령어는 인게임 플레이어만 사용할 수 있습니다.");
                return true;
            }

            if (args.length < 2) {
                player.sendMessage("§c[사용법] /house member <add|remove|list> [플레이어]");
                return true;
            }

            HouseData house = houseManager.getHouseByOwner(player.getUniqueId());
            if (house == null || house.getChunks().isEmpty()) {
                player.sendMessage("§c소유한 영지가 없습니다. 먼저 /house claim으로 땅을 등록하세요.");
                return true;
            }

            String memberAction = args[1].toLowerCase();

            if (memberAction.equals("list")) {
                if (house.getMembers().isEmpty()) {
                    player.sendMessage("§e등록된 멤버가 없습니다.");
                } else {
                    player.sendMessage("§a[영지 멤버 목록]");
                    for (UUID memberUuid : house.getMembers()) {
                        OfflinePlayer mp = Bukkit.getOfflinePlayer(memberUuid);
                        player.sendMessage("§7- §f" + (mp.getName() != null ? mp.getName() : memberUuid.toString()));
                    }
                }
                return true;
            }

            if (args.length < 3) {
                player.sendMessage("§c[사용법] /house member " + memberAction + " <플레이어>");
                return true;
            }

            OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[2]);
            if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
                player.sendMessage("§c자기 자신은 멤버로 지정할 수 없습니다.");
                return true;
            }

            if (memberAction.equals("add")) {
                house.addMember(targetPlayer.getUniqueId());
                player.sendMessage("§a" + args[2] + "님을 영지 멤버로 추가하였습니다.");
            } else if (memberAction.equals("remove")) {
                boolean removed = house.removeMember(targetPlayer.getUniqueId());
                if (removed) {
                    player.sendMessage("§e" + args[2] + "님을 영지 멤버에서 제거하였습니다.");
                } else {
                    player.sendMessage("§c해당 플레이어는 멤버 목록에 없습니다.");
                }
            } else {
                player.sendMessage("§c알 수 없는 멤버 명령어입니다: " + memberAction);
            }
            return true;
        }

        // 5. 서버 땅 관리: /house server <add|remove>
        if (sub.equals("server")) {
            if (!checkAdminPermission(sender)) return true;
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§c이 명령어는 인게임 플레이어만 사용할 수 있습니다.");
                return true;
            }

            if (args.length < 2) {
                player.sendMessage("§c[사용법] /house server <add|remove>");
                return true;
            }

            if (args[1].equalsIgnoreCase("add")) {
                boolean ok = houseManager.setServerChunk(player.getChunk());
                if (ok) player.sendMessage("§6현재 청크를 [서버 공용 구역]으로 등록하였습니다.");
                else player.sendMessage("§c이미 등록되어 있는 청크입니다.");
            } else if (args[1].equalsIgnoreCase("remove")) {
                boolean ok = houseManager.removeServerChunk(player.getChunk());
                if (ok) player.sendMessage("§e현재 청크의 서버 공용 구역 지정을 해제하였습니다.");
                else player.sendMessage("§c서버 공용 구역으로 지정된 청크가 아닙니다.");
            }
            return true;
        }

        // 6. 관리자용 강제 추가: /house add <플레이어|server>
        if (sub.equals("add")) {
            if (!checkAdminPermission(sender)) return true;
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§c이 명령어는 인게임 플레이어만 사용할 수 있습니다.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§c[사용법] /house add <플레이어|server>");
                return true;
            }

            if (args[1].equalsIgnoreCase("server")) {
                houseManager.setServerChunk(player.getChunk());
                sender.sendMessage("§6이 청크를 서버 공용 구역으로 설정하였습니다.");
            } else {
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                boolean ok = houseManager.claimChunk(target.getUniqueId(), player.getChunk());
                if (ok) sender.sendMessage("§a이 청크를 " + args[1] + "님의 땅으로 지정하였습니다.");
                else sender.sendMessage("§c청크 지정에 실패하였습니다.");
            }
            return true;
        }

        // 7. 관리자용 강제 삭제: /house remove <플레이어|server>
        if (sub.equals("remove")) {
            if (!checkAdminPermission(sender)) return true;
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§c이 명령어는 인게임 플레이어만 사용할 수 있습니다.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§c[사용법] /house remove <플레이어|server>");
                return true;
            }

            if (args[1].equalsIgnoreCase("server")) {
                houseManager.removeServerChunk(player.getChunk());
                sender.sendMessage("§e이 청크의 서버 공용 구역 지정을 해제하였습니다.");
            } else {
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                boolean ok = houseManager.unclaimChunk(target.getUniqueId(), player.getChunk());
                if (ok) sender.sendMessage("§e이 청크의 " + args[1] + "님 소유권을 해제하였습니다.");
                else sender.sendMessage("§c청크 소유권 해제에 실패하였습니다.");
            }
            return true;
        }

        sendHelpMessage(sender);
        return true;
    }

    public void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§6===== [ MineFarm 영지 시스템 도움말 ] =====");
        sender.sendMessage("§f/house who §7- 현재 서 있는 청크의 소유자 확인");
        sender.sendMessage("§f/house claim §7- 현재 서 있는 청크를 내 영지로 등록");
        sender.sendMessage("§f/house unclaim §7- 현재 서 있는 청크의 영지 등록 해제");
        sender.sendMessage("§f/house member <add|remove|list> §7- 영지 멤버 관리");
        if (sender.isOp()) {
            sender.sendMessage("§c[관리자 전용]");
            sender.sendMessage("§c/house server <add|remove> §7- 현재 청크를 서버 땅으로 등록/해제");
            sender.sendMessage("§c/house add <플레이어> §7- 현재 청크를 특정 유저의 땅으로 강제 지정");
            sender.sendMessage("§c/house remove <플레이어> §7- 현재 청크의 소유권 강제 해제");
        }
        sender.sendMessage("§6=========================================");
    }

    private boolean checkAdminPermission(CommandSender sender) {
        if (!sender.isOp()) {
            sender.sendMessage("§c이 명령어를 사용할 권한이 없습니다.");
            return false;
        }
        return true;
    }
}
