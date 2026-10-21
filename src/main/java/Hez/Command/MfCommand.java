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

        if (args.length > 0 && args[0].equalsIgnoreCase("wild")) {
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
                return true;
            }
            if (plugin.getWildManager() != null) {
                plugin.getWildManager().teleportToWild(player);
            } else {
                sender.sendMessage("§c[MineFarm] 야생 시스템이 활성화되지 않았습니다.");
            }
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("setwildspawn")) {
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
                return true;
            }
            if (plugin.getWildManager() != null) {
                plugin.getWildManager().setSpawnLocation(player.getLocation());
                sender.sendMessage("§a[MineFarm] 현재 위치가 야생 고정 스폰으로 설정되었습니다!");
            } else {
                sender.sendMessage("§c[MineFarm] 야생 시스템이 활성화되지 않았습니다.");
            }
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("cleanitems")) {
            if (plugin.getWildManager() != null && plugin.getWildManager().getItemCleaner() != null) {
                int count = plugin.getWildManager().getItemCleaner().cleanItems();
                sender.sendMessage("§a[MineFarm] 바닥 아이템 " + count + "개를 청소했습니다.");
            } else {
                sender.sendMessage("§c[MineFarm] 야생 아이템 청소기가 활성화되지 않았습니다.");
            }
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("colosseum")) {
            if (plugin.getColosseumManager() == null) {
                sender.sendMessage("§c[MineFarm] 콜로세움 시스템이 활성화되지 않았습니다.");
                return true;
            }
            Hez.PvP.ColosseumManager colosseum = plugin.getColosseumManager();

            if (args.length > 1 && args[1].equalsIgnoreCase("info")) {
                sender.sendMessage("§6=== 콜로세움 설정 정보 ===");
                sender.sendMessage("§7- 대상 마을 월드: §f" + colosseum.getTownWorldName());
                sender.sendMessage(String.format("§7- X 좌표 범위: §f%d ~ %d", colosseum.getMinX(), colosseum.getMaxX()));
                sender.sendMessage(String.format("§7- Z 좌표 범위: §f%d ~ %d", colosseum.getMinZ(), colosseum.getMaxZ()));
                java.util.List<org.bukkit.entity.Player> inside = colosseum.getInsideOnlinePlayers();
                sender.sendMessage("§7- 현재 내부 인원: §e" + inside.size() + "명" +
                        (inside.isEmpty() ? "" : " §7(" + inside.stream().map(p -> p.getName() + (colosseum.getSelectedClass(p.getUniqueId()) != null ? "(" + colosseum.getSelectedClass(p.getUniqueId()) + ")" : "")).collect(java.util.stream.Collectors.joining(", ")) + ")"));
                sender.sendMessage("§6--- 등록된 직업 선택 좌표 ---");
                for (java.util.Map.Entry<String, int[]> entry : colosseum.getClassPositions().entrySet()) {
                    int[] pos = entry.getValue();
                    sender.sendMessage(String.format("§e- %s: §f(X: %d, Z: %d)", entry.getKey(), pos[0], pos[1]));
                }
                sender.sendMessage("§6--- 등록된 팀 선택 구역 ---");
                for (java.util.Map.Entry<String, Hez.PvP.ColosseumManager.TeamArea> entry : colosseum.getTeamAreas().entrySet()) {
                    Hez.PvP.ColosseumManager.TeamArea area = entry.getValue();
                    sender.sendMessage(String.format("§e- %s: §f(X: %d ~ %d, Z: %d ~ %d)",
                            Hez.PvP.ColosseumManager.formatTeamName(entry.getKey()),
                            area.getMinX(), area.getMaxX(), area.getMinZ(), area.getMaxZ()));
                }
                return true;
            }

            if (args.length > 1 && args[1].equalsIgnoreCase("reload")) {
                colosseum.loadConfig();
                sender.sendMessage("§a[MineFarm] colosseumConfig.yml 설정을 다시 로드했습니다!");
                return true;
            }

            if (args.length > 2 && args[1].equalsIgnoreCase("setclass")) {
                if (!(sender instanceof org.bukkit.entity.Player player)) {
                    sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
                    return true;
                }
                String className = args[2];
                // 서 있는 블록 좌표 (X, Z)
                int x = player.getLocation().getBlockX();
                int z = player.getLocation().getBlockZ();
                colosseum.saveClassPosition(className, x, z);
                sender.sendMessage(String.format("§a[MineFarm] '%s' 직업의 선택 위치가 (X: %d, Z: %d)로 설정되었습니다!", className, x, z));
                return true;
            }

            if (args.length >= 5 && args[1].equalsIgnoreCase("setclasspos")) {
                String className = args[2];
                try {
                    int x = Integer.parseInt(args[3]);
                    int z = Integer.parseInt(args[4]);
                    colosseum.saveClassPosition(className, x, z);
                    sender.sendMessage(String.format("§a[MineFarm] '%s' 직업의 선택 위치가 (X: %d, Z: %d)로 설정되었습니다!", className, x, z));
                } catch (NumberFormatException e) {
                    sender.sendMessage("§c좌표는 정수로 입력해야 합니다. 사용법: /" + label + " colosseum setclasspos <직업명> <x> <z>");
                }
                return true;
            }

            if (args.length > 2 && args[1].equalsIgnoreCase("teampos1")) {
                if (!(sender instanceof org.bukkit.entity.Player player)) {
                    sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
                    return true;
                }
                String teamName = args[2].toLowerCase();
                int x = player.getLocation().getBlockX();
                int z = player.getLocation().getBlockZ();
                colosseum.setPlayerTeamPos1(player.getUniqueId(), x, z);
                sender.sendMessage(String.format("§a[MineFarm] '%s' 팀 구역 Pos1 설정: (X: %d, Z: %d). 이제 Pos2 위치로 가서 '/%s colosseum teampos2 %s'를 입력하세요.", teamName, x, z, label, teamName));
                return true;
            }

            if (args.length > 2 && args[1].equalsIgnoreCase("teampos2")) {
                if (!(sender instanceof org.bukkit.entity.Player player)) {
                    sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
                    return true;
                }
                String teamName = args[2].toLowerCase();
                int[] pos1 = colosseum.getPlayerTeamPos1(player.getUniqueId());
                if (pos1 == null) {
                    sender.sendMessage("§c먼저 '/" + label + " colosseum teampos1 " + teamName + "'을 실행하여 첫 번째 지점을 지정해주세요.");
                    return true;
                }
                int x2 = player.getLocation().getBlockX();
                int z2 = player.getLocation().getBlockZ();
                colosseum.saveTeamArea(teamName, pos1[0], pos1[1], x2, z2);
                colosseum.clearPlayerTeamPos1(player.getUniqueId());
                Hez.PvP.ColosseumManager.TeamArea area = colosseum.getTeamAreas().get(teamName);
                sender.sendMessage(String.format("§a[MineFarm] '%s' 팀 구역이 성공적으로 저장되었습니다! (X: %d ~ %d, Z: %d ~ %d)",
                        teamName, area.getMinX(), area.getMaxX(), area.getMinZ(), area.getMaxZ()));
                return true;
            }

            if (args.length >= 7 && args[1].equalsIgnoreCase("setteam")) {
                String teamName = args[2].toLowerCase();
                try {
                    int x1 = Integer.parseInt(args[3]);
                    int z1 = Integer.parseInt(args[4]);
                    int x2 = Integer.parseInt(args[5]);
                    int z2 = Integer.parseInt(args[6]);
                    colosseum.saveTeamArea(teamName, x1, z1, x2, z2);
                    Hez.PvP.ColosseumManager.TeamArea area = colosseum.getTeamAreas().get(teamName);
                    sender.sendMessage(String.format("§a[MineFarm] '%s' 팀 구역이 설정되었습니다: (X: %d ~ %d, Z: %d ~ %d)",
                            teamName, area.getMinX(), area.getMaxX(), area.getMinZ(), area.getMaxZ()));
                } catch (NumberFormatException e) {
                    sender.sendMessage("§c좌표는 정수로 입력해야 합니다. 사용법: /" + label + " colosseum setteam <팀명> <x1> <z1> <x2> <z2>");
                }
                return true;
            }

            if (args.length > 1 && args[1].equalsIgnoreCase("pos1")) {
                if (!(sender instanceof org.bukkit.entity.Player player)) {
                    sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
                    return true;
                }
                int x = player.getLocation().getBlockX();
                int z = player.getLocation().getBlockZ();
                colosseum.setPlayerPos1(player.getUniqueId(), x, z);
                sender.sendMessage(String.format("§a[MineFarm] 콜로세움 Pos1 설정: (X: %d, Z: %d). 이제 Pos2 위치로 가서 '/%s colosseum pos2'를 입력하세요.", x, z, label));
                return true;
            }

            if (args.length > 1 && args[1].equalsIgnoreCase("pos2")) {
                if (!(sender instanceof org.bukkit.entity.Player player)) {
                    sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
                    return true;
                }
                int[] pos1 = colosseum.getPlayerPos1(player.getUniqueId());
                if (pos1 == null) {
                    sender.sendMessage("§c먼저 '/" + label + " colosseum pos1'을 실행하여 첫 번째 지점을 지정해주세요.");
                    return true;
                }
                int x2 = player.getLocation().getBlockX();
                int z2 = player.getLocation().getBlockZ();
                colosseum.saveBounds(pos1[0], pos1[1], x2, z2);
                colosseum.clearPlayerPos1(player.getUniqueId());
                sender.sendMessage(String.format("§a[MineFarm] 콜로세움 영역이 성공적으로 저장되었습니다! (X: %d ~ %d, Z: %d ~ %d)",
                        colosseum.getMinX(), colosseum.getMaxX(), colosseum.getMinZ(), colosseum.getMaxZ()));
                return true;
            }

            if (args.length >= 6 && args[1].equalsIgnoreCase("setpos")) {
                try {
                    int x1 = Integer.parseInt(args[2]);
                    int z1 = Integer.parseInt(args[3]);
                    int x2 = Integer.parseInt(args[4]);
                    int z2 = Integer.parseInt(args[5]);
                    colosseum.saveBounds(x1, z1, x2, z2);
                    sender.sendMessage(String.format("§a[MineFarm] 콜로세움 영역이 설정되었습니다: (X: %d ~ %d, Z: %d ~ %d)",
                            colosseum.getMinX(), colosseum.getMaxX(), colosseum.getMinZ(), colosseum.getMaxZ()));
                } catch (NumberFormatException e) {
                    sender.sendMessage("§c좌표는 정수로 입력해야 합니다. 사용법: /" + label + " colosseum setpos <x1> <z1> <x2> <z2>");
                }
                return true;
            }

            sender.sendMessage("§6=== 콜로세움 관리 명령어 ===");
            sender.sendMessage("§e/" + label + " colosseum info §f- 콜로세움 정보 및 직업/팀 구역 확인");
            sender.sendMessage("§e/" + label + " colosseum pos1 §f- 현재 위치를 영역 시작 지점(Pos1)으로 지정");
            sender.sendMessage("§e/" + label + " colosseum pos2 §f- 현재 위치를 끝 지점(Pos2)으로 지정하고 저장");
            sender.sendMessage("§e/" + label + " colosseum setpos <x1> <z1> <x2> <z2> §f- 좌표 수동 설정");
            sender.sendMessage("§e/" + label + " colosseum setclass <직업명> §f- 현재 위치를 직업 선택 위치로 지정");
            sender.sendMessage("§e/" + label + " colosseum setclasspos <직업명> <x> <z> §f- 직업 위치 수동 설정");
            sender.sendMessage("§e/" + label + " colosseum teampos1 <팀명> §f- 현재 위치를 팀 구역 시작 지점(Pos1)으로 지정");
            sender.sendMessage("§e/" + label + " colosseum teampos2 <팀명> §f- 현재 위치를 팀 구역 끝 지점(Pos2)으로 지정하고 저장");
            sender.sendMessage("§e/" + label + " colosseum setteam <팀명> <x1> <z1> <x2> <z2> §f- 팀 구역 수동 설정");
            sender.sendMessage("§e/" + label + " colosseum reload §f- colosseumConfig.yml 다시 로드");
            return true;
        }

        sender.sendMessage("§6=== MineFarm 관리 명령어 ===");
        sender.sendMessage("§e/" + label + " reload §f- 플러그인 전체를 리로드합니다.");
        sender.sendMessage("§e/" + label + " wild §f- 야생 고정 스폰으로 이동합니다.");
        sender.sendMessage("§e/" + label + " setwildspawn §f- 현재 위치를 야생 고정 스폰으로 지정합니다.");
        sender.sendMessage("§e/" + label + " cleanitems §f- 야생 월드 바닥 아이템을 즉시 청소합니다.");
        sender.sendMessage("§e/" + label + " colosseum §f- 콜로세움 영역 및 설정 관리.");
        return true;
    }
}
