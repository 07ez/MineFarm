package Hez.Command;

import Hez.House.HouseManager;
import Hez.MineFarm.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class HouseCommand implements CommandExecutor {
    private final Main plugin;

    HouseManager houseManager;

    public HouseCommand(Main plugin) {
        this.plugin = plugin;
        houseManager = plugin.getHouseManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        // 집 도움말
        if (args[0].equalsIgnoreCase("help")) {
            sendHelpMessage(sender);
            return true;
        }

        // house add <플레이어>
        if (args[0].equalsIgnoreCase("add")) {
            if (!checkAdminPermission(sender)) return true;

            // 잘못 입력
            if (args.length < 2) {
                sender.sendMessage("§c[사용법] /house add <플레이어>");
                return true;
            }

            // 집 추가
            if(sender instanceof Player player) {
                if (args[1].equalsIgnoreCase("server")) { // 서버땅으로 지정
                    houseManager.SetServerChunk(player.getChunk());
                    sender.sendMessage("이 청크 공간을 server의 땅으로 정하였습니다.");
                }
                else {
                    houseManager.AddHouseChuck(player.getUniqueId(), player.getChunk());
                    sender.sendMessage("이 청크 공간을 " + player + "땅으로 정하였습니다.");
                }
            }
            else {
                sender.sendMessage("이 명령어는 인게임 플레이어만 사용할 수 있습니다.");
            }

        }

        // 집 삭제
        // house remove <플레이어>
        if (args[0].equalsIgnoreCase("remove")) {
            if (!checkAdminPermission(sender)) return true;

            // 잘못 입력
            if (args.length < 2) {
                sender.sendMessage("§c[사용법] /house remove <플레이어>");
                return true;
            }

            // 집 추가
            if(sender instanceof Player player) {
                if (args[1].equalsIgnoreCase("server")) { // 서버땅으로 지정
                    houseManager.RemoveServerChunk(player.getChunk());
                }
                else {
                    houseManager.RemoveChunk(player.getUniqueId(), player.getChunk());
                }
            }
            else {
                sender.sendMessage("이 명령어는 인게임 플레이어만 사용할 수 있습니다.");
            }

        }
        // 집 소유자 찾기
        // house who
        if(args[0].equalsIgnoreCase("who")) {
            if (!checkAdminPermission(sender)) return true;

            if(sender instanceof Player player) {
                sender.sendMessage(houseManager.GetHouseData(player.getLocation()).getOwner().toString() + "님의 땅입니다.");
            }
            else {
                sender.sendMessage("이 명령어는 인게임 플레이어만 사용 가능합니다.");
            }
        }
        return true;
    }

    public void sendHelpMessage(CommandSender sender) {

    }

    // 관리자 권한 체크
    private boolean checkAdminPermission(CommandSender sender) {
        if (!sender.isOp()) {
            sender.sendMessage("§c이 명령어를 사용할 권한이 없습니다.");
            return false;
        }
        return true;
    }
}
