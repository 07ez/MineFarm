package Hez.Command;

import Hez.Menu.CustomMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MenuCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            CustomMenu.open(player);
        } else {
            sender.sendMessage("§c이 명령어는 인게임 플레이어만 사용할 수 있습니다.");
        }
        return true;
    }
}
