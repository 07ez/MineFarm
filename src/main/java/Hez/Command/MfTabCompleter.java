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
            List<String> sub = List.of("reload", "wild", "setwildspawn", "cleanitems", "colosseum");
            List<String> result = new ArrayList<>();
            for (String s : sub) {
                if (s.toLowerCase().startsWith(args[0].toLowerCase())) {
                    result.add(s);
                }
            }
            return result;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("colosseum")) {
            List<String> sub = List.of("info", "pos1", "pos2", "setpos", "setclass", "setclasspos", "reload");
            List<String> result = new ArrayList<>();
            for (String s : sub) {
                if (s.toLowerCase().startsWith(args[1].toLowerCase())) {
                    result.add(s);
                }
            }
            return result;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("colosseum")
                && (args[1].equalsIgnoreCase("setclass") || args[1].equalsIgnoreCase("setclasspos"))) {
            List<String> classList = Hez.PvP.ColosseumManager.DEFAULT_CLASSES;
            List<String> result = new ArrayList<>();
            for (String c : classList) {
                if (c.toLowerCase().startsWith(args[2].toLowerCase())) {
                    result.add(c);
                }
            }
            return result;
        }

        return List.of();
    }
}
