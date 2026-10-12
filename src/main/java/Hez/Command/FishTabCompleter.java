package Hez.Command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FishTabCompleter implements TabCompleter {

    private static final List<String> SUB_COMMANDS = List.of("catch");
    private static final List<String> RARITIES = List.of("common", "rare", "epic", "legend");

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.isOp()) return List.of();

        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], SUB_COMMANDS, new ArrayList<>());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("catch")) {
            return StringUtil.copyPartialMatches(args[1], RARITIES, new ArrayList<>());
        }

        return List.of();
    }
}
