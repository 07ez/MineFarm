package Hez.Command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class MfTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("minefarm.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            List<String> sub = List.of("reload", "wild", "setwildspawn", "cleanitems");
            List<String> result = new ArrayList<>();
            for (String s : sub) {
                if (s.toLowerCase().startsWith(args[0].toLowerCase())) {
                    result.add(s);
                }
            }
            return result;
        }

        return List.of();
    }
}
