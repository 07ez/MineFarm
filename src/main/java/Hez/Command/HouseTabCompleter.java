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
import java.util.Collections;
import java.util.List;

public class HouseTabCompleter implements TabCompleter {
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        // 1. 첫 번째 인자 입력 중 (/house <여기>)
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            subCommands.add("help");
            // 관리자 권한(OP)이 있을 때만 지급, 차감 추천
            if (sender.isOp()) {
                subCommands.add("add");
                subCommands.add("remove");
            }

            return StringUtil.copyPartialMatches(args[0], subCommands, new ArrayList<>());
        }

        if (args.length == 2) {
            String sub = args[0];
            if (sub.equalsIgnoreCase("add") || sub.equalsIgnoreCase("remove")) {
                if (sender.isOp()) {
                    List<String> playerNames = new ArrayList<>();
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        playerNames.add(player.getName());
                    }
                    playerNames.add("server");
                    return StringUtil.copyPartialMatches(args[1], playerNames, new ArrayList<>());
                }
            }
            return Collections.emptyList(); // 다른 서브 명령어에서는 플레이어 목록 표시 안 함
        }

        return Collections.emptyList();
    }
}
