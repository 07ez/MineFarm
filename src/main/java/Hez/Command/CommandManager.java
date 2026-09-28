package Hez.Command;

import Hez.MineFarm.Main;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;

public class CommandManager {

    private final Main plugin;

    public CommandManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * 플러그인의 모든 명령어를 등록합니다.
     */
    public void registerCommands() {
        // 1. 명령어 등록
        register("money", new MoneyCommand(plugin), new MoneyTabCompleter());
        register("job", new JobCommand(plugin), new JobTabCompleter());
        register("house", new HouseCommand(plugin), new HouseTabCompleter());
    }

    /**
     * 명령어만 등록하는 헬퍼 메서드
     */
    private void register(String name, CommandExecutor executor) {
        register(name, executor, executor instanceof TabCompleter tab ? tab : null);
    }

    /**
     * 명령어와 TabCompleter를 함께 등록하는 헬퍼 메서드
     */
    private void register(String name, CommandExecutor executor, TabCompleter completer) {
        PluginCommand command = plugin.getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
            if (completer != null) {
                command.setTabCompleter(completer);
            }
        } else {
            plugin.getLogger().warning("plugin.yml에 '" + name + "' 명령어가 정의되어 있지 않습니다.");
        }
    }
}