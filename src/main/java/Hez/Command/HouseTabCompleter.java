package Hez.Command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HouseTabCompleter implements TabCompleter {
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        // 1. 첫 번째 인자 입력 중 (/house <여기>)
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(Arrays.asList("help", "who", "claim", "unclaim", "member"));
            if (sender.isOp()) {
                subCommands.add("server");
                subCommands.add("add");
                subCommands.add("remove");
            }
            return StringUtil.copyPartialMatches(args[0], subCommands, new ArrayList<>());
        }

        // 2. 두 번째 인자
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("member")) {
                return StringUtil.copyPartialMatches(args[1], Arrays.asList("add", "remove", "list"), new ArrayList<>());
            }
            if (sub.equals("server") && sender.isOp()) {
                return StringUtil.copyPartialMatches(args[1], Arrays.asList("add", "remove"), new ArrayList<>());
            }
            if ((sub.equals("add") || sub.equals("remove")) && sender.isOp()) {
                List<String> targets = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    targets.add(p.getName());
                }
                targets.add("server");
                return StringUtil.copyPartialMatches(args[1], targets, new ArrayList<>());
            }
            return Collections.emptyList();
        }

        // 3. 세 번째 인자 (/house member add/remove <플레이어>)
        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            String action = args[1].toLowerCase();
            if (sub.equals("member") && (action.equals("add") || action.equals("remove"))) {
                List<String> playerNames = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    playerNames.add(p.getName());
                }
                return StringUtil.copyPartialMatches(args[2], playerNames, new ArrayList<>());
            }
            return Collections.emptyList();
        }

        return Collections.emptyList();
    }
}
