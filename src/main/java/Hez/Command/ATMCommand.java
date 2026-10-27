package Hez.Command;

import Hez.Bank.ATMManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ATMCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }

        if (args.length == 0) {
            ATMManager.openATMMenu(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "deposit", "입금" -> ATMManager.startDeposit(player);
            case "withdraw", "출금" -> ATMManager.startWithdraw(player);
            case "cancel", "취소" -> {
                ATMManager.clearState(player.getUniqueId());
                player.sendMessage("§c[ATM] 이용이 취소되었습니다.");
            }
            default -> ATMManager.openATMMenu(player);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (String sub : List.of("deposit", "withdraw", "cancel", "입금", "출금", "취소")) {
                if (sub.startsWith(input)) {
                    list.add(sub);
                }
            }
        }
        return list;
    }
}
