package Hez.Command;

import Hez.MineFarm.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

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

        if (args.length > 0 && args[0].equalsIgnoreCase("tp")) {
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage("§e[MineFarm] 이동할 장소를 입력해주세요: §f/" + label + " tp <town | colosseum | wild>");
                return true;
            }

            String target = args[1].toLowerCase();
            switch (target) {
                case "town", "마을" -> {
                    boolean success = Hez.Menu.WarpManager.teleport(player, "town");
                    if (success) {
                        sender.sendMessage("§a[MineFarm] 마을로 이동했습니다.");
                    } else {
                        sender.sendMessage("§c[MineFarm] 마을 월드 스폰으로 이동하지 못했습니다.");
                    }
                }
                case "colosseum", "arena", "pvp", "콜로세움" -> {
                    if (plugin.getColosseumManager() != null) {
                        plugin.getColosseumManager().teleportToArena(player);
                    } else {
                        sender.sendMessage("§c[MineFarm] 콜로세움 시스템이 활성화되지 않았습니다.");
                    }
                }
                case "wild", "야생" -> {
                    if (plugin.getWildManager() != null) {
                        plugin.getWildManager().teleportToWild(player);
                    } else {
                        sender.sendMessage("§c[MineFarm] 야생 시스템이 활성화되지 않았습니다.");
                    }
                }
                default -> {
                    sender.sendMessage("§c[MineFarm] 알 수 없는 목적지입니다: " + args[1]);
                    sender.sendMessage("§e사용 가능한 목적지: §ftown, colosseum, wild");
                }
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

        if (args.length > 0 && (args[0].equalsIgnoreCase("arena") || args[0].equalsIgnoreCase("아레나"))) {
            if (plugin.getColosseumManager() == null || plugin.getColosseumManager().getTemplateManager() == null) {
                sender.sendMessage("§c[MineFarm] 콜로세움 아레나 시스템이 활성화되지 않았습니다.");
                return true;
            }
            Hez.PvP.ColosseumManager colosseum = plugin.getColosseumManager();
            Hez.PvP.ArenaTemplateManager arena = colosseum.getTemplateManager();

            if (args.length > 1) {
                String sub = args[1].toLowerCase();

                // /mf arena savemain <x1> <y1> <z1> <x2> <y2> <z2>
                if (sub.equals("savemain")) {
                    if (args.length < 8) {
                        sender.sendMessage("§c사용법: /" + label + " arena savemain <x1> <y1> <z1> <x2> <y2> <z2>");
                        return true;
                    }
                    try {
                        int x1 = Integer.parseInt(args[2]);
                        int y1 = Integer.parseInt(args[3]);
                        int z1 = Integer.parseInt(args[4]);
                        int x2 = Integer.parseInt(args[5]);
                        int y2 = Integer.parseInt(args[6]);
                        int z2 = Integer.parseInt(args[7]);

                        org.bukkit.World targetWorld = (sender instanceof org.bukkit.entity.Player p) ? p.getWorld() : colosseum.getArenaWorld();
                        if (targetWorld == null) {
                            sender.sendMessage("§c대상 월드를 찾을 수 없습니다.");
                            return true;
                        }

                        org.bukkit.Location loc1 = new org.bukkit.Location(targetWorld, x1, y1, z1);
                        org.bukkit.Location loc2 = new org.bukkit.Location(targetWorld, x2, y2, z2);

                        boolean ok = arena.saveTemplate("main", loc1, loc2);
                        if (ok) {
                            sender.sendMessage("§a[아레나] 기본 경기장(main) 템플릿이 성공적으로 저장되었습니다! (Air 무시 저장 완료)");
                        } else {
                            sender.sendMessage("§c[아레나] 기본 경기장 저장 중 오류가 발생했습니다.");
                        }
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§c좌표는 정수로 입력해주세요: /" + label + " arena savemain <x1> <y1> <z1> <x2> <y2> <z2>");
                    }
                    return true;
                }

                // /mf arena savemap <x1> <y1> <z1> <x2> <y2> <z2> <이름> [-void]
                if (sub.equals("savemap")) {
                    if (args.length < 9) {
                        sender.sendMessage("§c사용법: /" + label + " arena savemap <x1> <y1> <z1> <x2> <y2> <z2> <맵이름> [-void]");
                        return true;
                    }
                    try {
                        int x1 = Integer.parseInt(args[2]);
                        int y1 = Integer.parseInt(args[3]);
                        int z1 = Integer.parseInt(args[4]);
                        int x2 = Integer.parseInt(args[5]);
                        int y2 = Integer.parseInt(args[6]);
                        int z2 = Integer.parseInt(args[7]);
                        String mapName = args[8];
                        boolean isVoid = (args.length >= 10 && (args[9].equalsIgnoreCase("-void") || args[9].equalsIgnoreCase("void") || args[9].equalsIgnoreCase("--void")));

                        org.bukkit.World targetWorld = (sender instanceof org.bukkit.entity.Player p) ? p.getWorld() : colosseum.getArenaWorld();
                        if (targetWorld == null) {
                            sender.sendMessage("§c대상 월드를 찾을 수 없습니다.");
                            return true;
                        }

                        org.bukkit.Location loc1 = new org.bukkit.Location(targetWorld, x1, y1, z1);
                        org.bukkit.Location loc2 = new org.bukkit.Location(targetWorld, x2, y2, z2);

                        boolean ok = arena.saveTemplate(mapName, loc1, loc2, isVoid);
                        if (ok) {
                            if (isVoid) {
                                sender.sendMessage("§a[아레나] 공허(낙사) 테마 경기장(" + mapName + ") 템플릿이 성공적으로 저장되었습니다! (바닥 공기화 모드 적용)");
                            } else {
                                sender.sendMessage("§a[아레나] 테마 경기장(" + mapName + ") 템플릿이 성공적으로 저장되었습니다!");
                            }
                        } else {
                            sender.sendMessage("§c[아레나] 테마 경기장 저장 중 오류가 발생했습니다.");
                        }
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§c좌표는 정수로 입력해주세요: /" + label + " arena savemap <x1> <y1> <z1> <x2> <y2> <z2> <맵이름> [-void]");
                    }
                    return true;
                }

                // /mf arena setbounds <x1> <y1> <z1> <x2> <y2> <z2>
                if (sub.equals("setbounds") || sub.equals("bounds")) {
                    if (args.length < 8) {
                        sender.sendMessage("§c사용법: /" + label + " arena setbounds <x1> <y1> <z1> <x2> <y2> <z2>");
                        return true;
                    }
                    try {
                        int x1 = Integer.parseInt(args[2]);
                        int y1 = Integer.parseInt(args[3]);
                        int z1 = Integer.parseInt(args[4]);
                        int x2 = Integer.parseInt(args[5]);
                        int y2 = Integer.parseInt(args[6]);
                        int z2 = Integer.parseInt(args[7]);
                        arena.setBounds(x1, y1, z1, x2, y2, z2);
                        sender.sendMessage(String.format("§a[아레나] 전투 영역 경계가 설정되었습니다: (X: %d~%d, Y: %d~%d, Z: %d~%d)",
                                arena.getBoundsMinX(), arena.getBoundsMaxX(),
                                arena.getBoundsMinY(), arena.getBoundsMaxY(),
                                arena.getBoundsMinZ(), arena.getBoundsMaxZ()));
                        sender.sendMessage("§7(이 경계 밖의 프리셋 건축물 및 외벽은 파괴 및 리셋으로부터 완전히 보호됩니다)");
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§c좌표는 정수로 입력해주세요: /" + label + " arena setbounds <x1> <y1> <z1> <x2> <y2> <z2>");
                    }
                    return true;
                }

                // /mf arena setpastepos [x] [y] [z]
                if (sub.equals("setpastepos") || sub.equals("setcenter")) {
                    if (args.length >= 5) {
                        try {
                            int x = Integer.parseInt(args[2]);
                            int y = Integer.parseInt(args[3]);
                            int z = Integer.parseInt(args[4]);
                            org.bukkit.World w = (sender instanceof org.bukkit.entity.Player p) ? p.getWorld() : colosseum.getArenaWorld();
                            arena.setPasteLocation(new org.bukkit.Location(w, x, y, z));
                            sender.sendMessage("§a[아레나] 고정 붙여넣기 기준 좌표가 설정되었습니다: (" + x + ", " + y + ", " + z + ")");
                        } catch (NumberFormatException e) {
                            sender.sendMessage("§c좌표는 정수로 입력해주세요: /" + label + " arena setpastepos <x> <y> <z>");
                        }
                    } else if (sender instanceof org.bukkit.entity.Player player) {
                        org.bukkit.Location loc = player.getLocation().getBlock().getLocation();
                        arena.setPasteLocation(loc);
                        sender.sendMessage(String.format("§a[아레나] 현재 위치가 고정 붙여넣기 기준 좌표로 설정되었습니다: (%d, %d, %d)",
                                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
                    } else {
                        sender.sendMessage("§c콘솔에서는 좌표를 입력해야 합니다: /" + label + " arena setpastepos <x> <y> <z>");
                    }
                    return true;
                }

                // /mf arena reset
                if (sub.equals("reset")) {
                    boolean ok = arena.resetToBaseArena();
                    if (ok) {
                        sender.sendMessage("§a[아레나] 경기장을 기본 경기장(main) 상태로 원상복구했습니다! (Air 무시 적용)");
                    } else {
                        sender.sendMessage("§c[아레나] 복구 실패: 기준 좌표(setpastepos) 또는 main 템플릿이 설정되었는지 확인하세요.");
                    }
                    return true;
                }

                // /mf arena loadmap <이름>
                if (sub.equals("loadmap") || sub.equals("paste") || sub.equals("testmap")) {
                    if (args.length < 3) {
                        sender.sendMessage("§c사용법: /" + label + " arena loadmap <맵이름>");
                        return true;
                    }
                    String mapName = args[2];
                    boolean ok = arena.pasteTemplate(mapName, true);
                    if (ok) {
                        sender.sendMessage("§a[아레나] 테마 경기장(" + mapName + ")을 Air 무시로 붙여넣었습니다.");
                    } else {
                        sender.sendMessage("§c[아레나] 붙여넣기 실패: 해당 맵이 없거나 기준 좌표(setpastepos)가 설정되지 않았습니다.");
                    }
                    return true;
                }

                // /mf arena listmaps
                if (sub.equals("listmaps") || sub.equals("list")) {
                    java.util.Set<String> maps = arena.getRegisteredMaps();
                    sender.sendMessage("§6=== 등록된 테마 경기장 목록 ===");
                    sender.sendMessage("§7- 기본 경기장(main): " + (arena.hasMainTemplate() ? "§a등록됨" : "§c미등록"));
                    if (maps.isEmpty()) {
                        sender.sendMessage("§7- 등록된 테마 경기장이 없습니다.");
                    } else {
                        List<String> formatted = new ArrayList<>();
                        for (String m : maps) {
                            Hez.PvP.ArenaTemplateManager.ArenaTemplate tpl = arena.loadTemplate(m);
                            if (tpl != null && tpl.isVoidMode) {
                                formatted.add(m + "§8(공허/낙사)§f");
                            } else {
                                formatted.add(m);
                            }
                        }
                        sender.sendMessage("§e- 테마 맵 (" + maps.size() + "개): §f" + String.join(", ", formatted));
                    }
                    return true;
                }

                // /mf arena delmap <이름>
                if (sub.equals("delmap") || sub.equals("delete")) {
                    if (args.length < 3) {
                        sender.sendMessage("§c사용법: /" + label + " arena delmap <맵이름>");
                        return true;
                    }
                    String mapName = args[2];
                    boolean ok = arena.deleteTemplate(mapName);
                    if (ok) {
                        sender.sendMessage("§a[아레나] 경기장(" + mapName + ") 템플릿이 삭제되었습니다.");
                    } else {
                        sender.sendMessage("§c[아레나] 해당 템플릿 파일을 찾을 수 없습니다.");
                    }
                    return true;
                }

                // /mf arena setspawn <team1|team2|team3|team4|spectator> [x] [y] [z]
                if (sub.equals("setspawn")) {
                    if (args.length < 3) {
                        sender.sendMessage("§c사용법: /" + label + " arena setspawn <team1|team2|team3|team4|spectator> [x] [y] [z]");
                        return true;
                    }
                    String teamKey = args[2].toLowerCase();
                    if (args.length >= 6) {
                        try {
                            double x = Double.parseDouble(args[3]);
                            double y = Double.parseDouble(args[4]);
                            double z = Double.parseDouble(args[5]);
                            org.bukkit.World w = (sender instanceof org.bukkit.entity.Player p) ? p.getWorld() : colosseum.getArenaWorld();
                            arena.setTeamSpawn(teamKey, new org.bukkit.Location(w, x, y, z));
                            sender.sendMessage("§a[아레나] " + teamKey + " 스폰 위치가 설정되었습니다.");
                        } catch (NumberFormatException e) {
                            sender.sendMessage("§c좌표는 숫자로 입력해주세요.");
                        }
                    } else if (sender instanceof org.bukkit.entity.Player player) {
                        arena.setTeamSpawn(teamKey, player.getLocation());
                        sender.sendMessage("§a[아레나] 현재 위치가 " + teamKey + " 스폰 위치로 설정되었습니다.");
                    } else {
                        sender.sendMessage("§c콘솔에서는 좌표를 입력해야 합니다.");
                    }
                    return true;
                }

                // /mf arena info
                if (sub.equals("info")) {
                    sender.sendMessage("§6=== 아레나 템플릿 & 스폰 설정 정보 ===");
                    if (arena.hasBounds()) {
                        sender.sendMessage(String.format("§7- 전투 영역 경계(Bounds): §fX: %d~%d, Y: %d~%d, Z: %d~%d",
                                arena.getBoundsMinX(), arena.getBoundsMaxX(),
                                arena.getBoundsMinY(), arena.getBoundsMaxY(),
                                arena.getBoundsMinZ(), arena.getBoundsMaxZ()));
                    } else {
                        sender.sendMessage("§7- 전투 영역 경계(Bounds): §c미설정 (/mf arena setbounds 로 설정)");
                    }
                    org.bukkit.Location pLoc = arena.getPasteLocation();
                    sender.sendMessage("§7- 붙여넣기 기준점: " + (pLoc != null ? String.format("§f(X: %d, Y: %d, Z: %d)", pLoc.getBlockX(), pLoc.getBlockY(), pLoc.getBlockZ()) : "§c미설정"));
                    sender.sendMessage("§7- 기본 경기장(main): " + (arena.hasMainTemplate() ? "§a등록됨" : "§c미등록 (/mf arena savemain 으로 등록)"));
                    sender.sendMessage("§7- 등록된 테마 맵: §e" + arena.getRegisteredMaps().size() + "개 §f(" + String.join(", ", arena.getRegisteredMaps()) + ")");
                    sender.sendMessage("§6--- 팀별 소환 좌표 ---");
                    for (String key : java.util.List.of("team1", "team2", "team3", "team4", "spectator")) {
                        org.bukkit.Location sLoc = arena.getTeamSpawn(key);
                        if (sLoc != null) {
                            sender.sendMessage(String.format("§e- %s: §f(X: %.1f, Y: %.1f, Z: %.1f)", key, sLoc.getX(), sLoc.getY(), sLoc.getZ()));
                        } else {
                            sender.sendMessage("§7- " + key + ": §c미설정");
                        }
                    }
                    return true;
                }
            }

            sender.sendMessage("§6=== 아레나 관리 명령어 ===");
            sender.sendMessage("§e/" + label + " arena info §f- 아레나 템플릿 및 스폰 설정 정보 확인");
            sender.sendMessage("§e/" + label + " arena setbounds <x1> <y1> <z1> <x2> <y2> <z2> §f- 전투 영역 경계 설정 (외부 프리셋 보호)");
            sender.sendMessage("§e/" + label + " arena setpastepos [x y z] §f- 경기장 붙여넣기 기준 좌표 설정");
            sender.sendMessage("§e/" + label + " arena savemain <x1> <y1> <z1> <x2> <y2> <z2> §f- 기본 경기장 저장");
            sender.sendMessage("§e/" + label + " arena savemap <x1> <y1> <z1> <x2> <y2> <z2> <이름> [-void] §f- 테마 경기장 저장 (-void: 공허/낙사 맵)");
            sender.sendMessage("§e/" + label + " arena loadmap <이름> §f- 테마 경기장 Air 무시 붙여넣기 테스트");
            sender.sendMessage("§e/" + label + " arena reset §f- 기본 경기장으로 원상복구 테스트");
            sender.sendMessage("§e/" + label + " arena listmaps §f- 등록된 테마 맵 목록 확인");
            sender.sendMessage("§e/" + label + " arena delmap <이름> §f- 테마 맵 삭제");
            sender.sendMessage("§e/" + label + " arena setspawn <team1..4|spectator> [x y z] §f- 팀별 소환 위치 설정");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("colosseum")) {
            if (plugin.getColosseumManager() == null) {
                sender.sendMessage("§c[MineFarm] 콜로세움 시스템이 활성화되지 않았습니다.");
                return true;
            }
            Hez.PvP.ColosseumManager colosseum = plugin.getColosseumManager();

            if (args.length > 1 && args[1].equalsIgnoreCase("tp")) {
                if (!(sender instanceof org.bukkit.entity.Player player)) {
                    sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
                    return true;
                }
                colosseum.teleportToArena(player);
                return true;
            }

            if (args.length > 1 && (args[1].equalsIgnoreCase("setarenaspawn") || args[1].equalsIgnoreCase("setspawn"))) {
                if (!(sender instanceof org.bukkit.entity.Player player)) {
                    sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
                    return true;
                }
                colosseum.setArenaSpawnLocation(player.getLocation());
                sender.sendMessage("§a[콜로세움] 현재 위치가 결투장 평지 월드 스폰 위치로 설정되었습니다!");
                return true;
            }

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
            sender.sendMessage("§e/" + label + " colosseum tp §f- 결투장 평지 월드로 이동");
            sender.sendMessage("§e/" + label + " colosseum setarenaspawn §f- 현재 위치를 결투장 스폰으로 설정");
            sender.sendMessage("§e/" + label + " colosseum pos1 §f- 현재 위치를 대기실 시작 지점(Pos1)으로 지정");
            sender.sendMessage("§e/" + label + " colosseum pos2 §f- 현재 위치를 대기실 끝 지점(Pos2)으로 지정하고 저장");
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
        sender.sendMessage("§e/" + label + " tp <town | colosseum | wild> §f- 지정된 장소로 텔레포트합니다.");
        sender.sendMessage("§e/" + label + " wild §f- 야생 고정 스폰으로 이동합니다.");
        sender.sendMessage("§e/" + label + " setwildspawn §f- 현재 위치를 야생 고정 스폰으로 지정합니다.");
        sender.sendMessage("§e/" + label + " cleanitems §f- 야생 월드 바닥 아이템을 즉시 청소합니다.");
        sender.sendMessage("§e/" + label + " colosseum §f- 콜로세움 대기실 영역 및 설정 관리.");
        sender.sendMessage("§e/" + label + " arena §f- 결투장 템플릿(main, 테마 맵) 및 스폰 관리.");
        return true;
    }
}
