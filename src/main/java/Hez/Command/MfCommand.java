package Hez.Command;

import Hez.MineFarm.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MfCommand implements CommandExecutor {

    private final Main plugin;

    public MfCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("minefarm.admin")) {
            sender.sendMessage("§c이 명령어를 실행할 권한이 없습니다.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage("§e[MineFarm] 플러그인 리로드를 시작합니다...");
            long start = System.currentTimeMillis();
            try {
                plugin.reloadPlugin();
                long elapsed = System.currentTimeMillis() - start;
                sender.sendMessage("§a[MineFarm] 모든 데이터, 레시피, 리스너 및 설정이 성공적으로 리로드되었습니다! (" + elapsed + "ms)");
            } catch (Exception e) {
                sender.sendMessage("§c[MineFarm] 리로드 중 오류가 발생했습니다: " + e.getMessage());
                plugin.getLogger().severe("리로드 중 오류 발생: " + e.getMessage());
                e.printStackTrace();
            }
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("wild")) {
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
                return true;
            }
            if (plugin.getWildManager() != null) {
                plugin.getWildManager().teleportToWild(player);
            } else {
                sender.sendMessage("§c[MineFarm] 야생 시스템이 활성화되지 않았습니다.");
            }
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("setwildspawn")) {
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
                return true;
            }
            if (plugin.getWildManager() != null) {
                plugin.getWildManager().setSpawnLocation(player.getLocation());
                sender.sendMessage("§a[MineFarm] 현재 위치가 야생 고정 스폰으로 설정되었습니다!");
            } else {
                sender.sendMessage("§c[MineFarm] 야생 시스템이 활성화되지 않았습니다.");
            }
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("cleanitems")) {
            if (plugin.getWildManager() != null && plugin.getWildManager().getItemCleaner() != null) {
                int count = plugin.getWildManager().getItemCleaner().cleanItems();
                sender.sendMessage("§a[MineFarm] 바닥 아이템 " + count + "개를 청소했습니다.");
            } else {
                sender.sendMessage("§c[MineFarm] 야생 아이템 청소기가 활성화되지 않았습니다.");
            }
            return true;
        }

        sender.sendMessage("§6=== MineFarm 관리 명령어 ===");
        sender.sendMessage("§e/" + label + " reload §f- 플러그인 전체를 리로드합니다.");
        sender.sendMessage("§e/" + label + " wild §f- 야생 고정 스폰으로 이동합니다.");
        sender.sendMessage("§e/" + label + " setwildspawn §f- 현재 위치를 야생 고정 스폰으로 지정합니다.");
        sender.sendMessage("§e/" + label + " cleanitems §f- 야생 월드 바닥 아이템을 즉시 청소합니다.");
        return true;
    }
}
