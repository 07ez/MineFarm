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

        sender.sendMessage("§6=== MineFarm 관리 명령어 ===");
        sender.sendMessage("§e/" + label + " reload §f- 플러그인 데이터, 레시피, 디스플레이 및 설정을 전체 리로드합니다.");
        return true;
    }
}
