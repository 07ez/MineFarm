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
            List<String> sub = List.of("reload", "tp", "wild", "setwildspawn", "cleanitems", "colosseum", "arena", "bde");
            List<String> result = new ArrayList<>();
            for (String s : sub) {
                if (s.toLowerCase().startsWith(args[0].toLowerCase())) {
                    result.add(s);
                }
            }
            return result;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("bde")) {
            String[] bdeArgs = new String[args.length - 1];
            System.arraycopy(args, 1, bdeArgs, 0, bdeArgs.length);
            Hez.MineFarm.Main main = Hez.MineFarm.Main.getInstance();
            if (main != null && main.getBdEngineManager() != null) {
                return new Hez.BDEngine.BdeCommand(main, main.getBdEngineManager()).onTabComplete(sender, command, alias, bdeArgs);
            }
            return List.of();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("tp")) {
            List<String> sub = List.of("town", "colosseum", "wild");
            List<String> result = new ArrayList<>();
            for (String s : sub) {
                if (s.toLowerCase().startsWith(args[1].toLowerCase())) {
                    result.add(s);
                }
            }
            return result;
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("arena") || args[0].equalsIgnoreCase("아레나"))) {
            List<String> sub = List.of("info", "setbounds", "setpastepos", "savemain", "savemap", "loadmap", "reset", "listmaps", "delmap", "setspawn");
            List<String> result = new ArrayList<>();
            for (String s : sub) {
                if (s.toLowerCase().startsWith(args[1].toLowerCase())) {
                    result.add(s);
                }
            }
            return result;
        }

        if (args.length == 3 && (args[0].equalsIgnoreCase("arena") || args[0].equalsIgnoreCase("아레나"))) {
            if (args[1].equalsIgnoreCase("setspawn")) {
                List<String> sub = List.of("team1", "team2", "team3", "team4", "spectator");
                List<String> result = new ArrayList<>();
                for (String s : sub) {
                    if (s.toLowerCase().startsWith(args[2].toLowerCase())) {
                        result.add(s);
                    }
                }
                return result;
            }
            if (args[1].equalsIgnoreCase("loadmap") || args[1].equalsIgnoreCase("delmap") || args[1].equalsIgnoreCase("paste") || args[1].equalsIgnoreCase("testmap")) {
                if (Hez.MineFarm.Main.getInstance() != null &&
                        Hez.MineFarm.Main.getInstance().getColosseumManager() != null &&
                        Hez.MineFarm.Main.getInstance().getColosseumManager().getTemplateManager() != null) {
                    List<String> result = new ArrayList<>();
                    for (String m : Hez.MineFarm.Main.getInstance().getColosseumManager().getTemplateManager().getRegisteredMaps()) {
                        if (m.toLowerCase().startsWith(args[2].toLowerCase())) {
                            result.add(m);
                        }
                    }
                    return result;
                }
            }
        }

        if (args.length == 10 && (args[0].equalsIgnoreCase("arena") || args[0].equalsIgnoreCase("아레나"))) {
            if (args[1].equalsIgnoreCase("savemap")) {
                if ("-void".startsWith(args[9].toLowerCase())) {
                    return List.of("-void");
                }
            }
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("colosseum")) {
            List<String> sub = List.of("start", "info", "tp", "setarenaspawn", "pos1", "pos2", "setpos", "setclass", "setclasspos", "teampos1", "teampos2", "setteam", "reload");
            List<String> result = new ArrayList<>();
            for (String s : sub) {
                if (s.toLowerCase().startsWith(args[1].toLowerCase())) {
                    result.add(s);
                }
            }
            return result;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("colosseum")) {
            if (args[1].equalsIgnoreCase("setclass") || args[1].equalsIgnoreCase("setclasspos")) {
                List<String> classList = Hez.PvP.ColosseumManager.DEFAULT_CLASSES;
                List<String> result = new ArrayList<>();
                for (String c : classList) {
                    if (c.toLowerCase().startsWith(args[2].toLowerCase())) {
                        result.add(c);
                    }
                }
                return result;
            }

            if (args[1].equalsIgnoreCase("teampos1") || args[1].equalsIgnoreCase("teampos2") || args[1].equalsIgnoreCase("setteam")) {
                List<String> teamList = Hez.PvP.ColosseumManager.DEFAULT_TEAMS;
                List<String> result = new ArrayList<>();
                for (String t : teamList) {
                    if (t.toLowerCase().startsWith(args[2].toLowerCase())) {
                        result.add(t);
                    }
                }
                return result;
            }
        }

        return List.of();
    }
}
