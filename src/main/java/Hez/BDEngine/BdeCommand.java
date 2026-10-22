package Hez.BDEngine;

import Hez.MineFarm.Main;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * BDEngine 관리 및 빠른 소환 명령어 실행기
 * - /bde download <ID> <모델명> <태그1> <태그2>
 * - /bde spawn <모델명>
 * - /bde remove <태그> [반경]
 * - /bde list
 * - /bde reload
 */
public class BdeCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final BDEngineManager manager;

    public BdeCommand(Main plugin, BDEngineManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("minefarm.admin")) {
            sender.sendMessage("§c이 명령어를 실행할 권한이 없습니다.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "download", "다운로드" -> {
                if (args.length < 5) {
                    sender.sendMessage("§c사용법: /" + label + " download <ID> <모델명> <태그1> <태그2>");
                    sender.sendMessage("§7예시: /" + label + " download 123456 Blub Pet Blub");
                    return true;
                }
                String exportId = args[1];
                String modelName = args[2];
                String tag1 = args[3];
                String tag2 = args[4];

                sender.sendMessage("§e[BDEngine] ID " + exportId + " 모델 다운로드를 요청 중입니다...");
                manager.downloadModel(exportId, modelName, tag1, tag2, (success, message) -> {
                    if (success) {
                        sender.sendMessage("§a[BDEngine] " + message);
                        sender.sendMessage("§b이제 /" + label + " spawn " + modelName + " 명령어로 언제든 소환할 수 있습니다.");
                    } else {
                        sender.sendMessage("§c[BDEngine] " + message);
                    }
                });
                return true;
            }

            case "spawn", "스폰", "소환" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§c사용법: /" + label + " spawn <모델명>");
                    return true;
                }
                String modelName = args[1];
                BDEngineModel model = manager.getModel(modelName);

                if (model == null) {
                    // 혹시 숫자 ID를 바로 입력했을 경우 안내
                    if (modelName.matches("\\d+")) {
                        sender.sendMessage("§e[BDEngine] '" + modelName + "'은(는) 저장된 모델명이 아닙니다.");
                        sender.sendMessage("§7웹에서 새로 받은 ID라면 먼저 다운로드해주세요: §f/" + label + " download " + modelName + " <모델명> <태그1> <태그2>");
                    } else {
                        sender.sendMessage("§c[BDEngine] '" + modelName + "' 모델을 찾을 수 없습니다. (먼저 download 해주세요)");
                    }
                    return true;
                }

                Location spawnLoc = player.getLocation();
                boolean spawned = manager.spawnModel(spawnLoc, modelName);
                if (spawned) {
                    player.sendMessage("§a[BDEngine] 모델 '" + model.getName() + "' 소환 완료! (태그1: " + model.getTag1() + ", 태그2: " + model.getTag2() + ")");
                } else {
                    player.sendMessage("§c[BDEngine] 모델 소환에 실패했습니다.");
                }
                return true;
            }

            case "remove", "삭제", "제거" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§c사용법: /" + label + " remove <태그> [반경]");
                    sender.sendMessage("§7예시: /" + label + " remove Tree 10  (주변 10블록 내 Tree 태그 BDEngine 엔티티만 안전 삭제)");
                    return true;
                }

                String targetTag = args[1];
                double radius = 10.0;
                if (args.length >= 3) {
                    try {
                        radius = Double.parseDouble(args[2]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§c반경은 올바른 숫자여야 합니다.");
                        return true;
                    }
                }

                int removed = manager.removeModels(player.getLocation(), targetTag, radius);
                player.sendMessage("§a[BDEngine] 반경 " + radius + "블록 내 태그 '" + targetTag + "' BDEngine 엔티티 " + removed + "개를 안전하게 제거했습니다.");
                return true;
            }

            case "list", "목록" -> {
                List<String> names = manager.getModelNames();
                if (names.isEmpty()) {
                    sender.sendMessage("§e[BDEngine] 현재 저장된 모델이 없습니다. (/bde download 로 모델을 다운로드하세요)");
                    return true;
                }
                sender.sendMessage("§6=== [BDEngine 저장된 모델 목록 (" + names.size() + "개)] ===");
                for (String name : names) {
                    BDEngineModel m = manager.getModel(name);
                    if (m != null) {
                        sender.sendMessage("§f- §a" + m.getName() + " §7(태그1: §e" + m.getTag1() + "§7, 태그2: §e" + m.getTag2() + "§7)");
                    }
                }
                return true;
            }

            case "reload", "리로드" -> {
                manager.init();
                sender.sendMessage("§a[BDEngine] 모델 파일 및 태그 목록을 디스크에서 다시 로드했습니다.");
                return true;
            }

            default -> {
                sendHelp(sender, label);
                return true;
            }
        }
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage("§6=== [BDEngine 명령어 안내] ===");
        sender.sendMessage("§f/" + label + " download <ID> <모델명> <태그1> <태그2> §7- BDEngine API에서 모델 다운로드 및 저장");
        sender.sendMessage("§f/" + label + " spawn <모델명> §7- 플레이어 위치에 모델 소환");
        sender.sendMessage("§f/" + label + " remove <태그> [반경] §7- 해당 태그의 BDEngine 모델 엔티티만 안전 삭제 (기본 10블록)");
        sender.sendMessage("§f/" + label + " list §7- 저장된 모델 목록 조회");
        sender.sendMessage("§f/" + label + " reload §7- 모델 및 태그 리로드");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("minefarm.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            List<String> sub = List.of("download", "spawn", "remove", "list", "reload");
            return filterStartsWith(sub, args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("spawn") || sub.equals("스폰") || sub.equals("소환")) {
                // 모델명 리스트 자동완성
                return filterStartsWith(manager.getModelNames(), args[1]);
            } else if (sub.equals("remove") || sub.equals("삭제") || sub.equals("제거")) {
                // 등록된 태그 리스트 자동완성
                List<String> tags = new ArrayList<>(manager.getRegisteredTags());
                tags.add("all");
                return filterStartsWith(tags, args[1]);
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("remove") || sub.equals("삭제") || sub.equals("제거")) {
                return filterStartsWith(List.of("5", "10", "20", "50"), args[2]);
            }
        }

        if (args.length == 4) {
            String sub = args[0].toLowerCase();
            if (sub.equals("download") || sub.equals("다운로드")) {
                // 태그1 자동완성에 기존 태그 목록 추천
                return filterStartsWith(new ArrayList<>(manager.getRegisteredTags()), args[3]);
            }
        }

        // 태그2(args.length == 5)는 사용자 요청에 따라 자동완성에 띄우지 않음

        return List.of();
    }

    private List<String> filterStartsWith(List<String> candidates, String prefix) {
        String lower = prefix.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String c : candidates) {
            if (c.toLowerCase().startsWith(lower)) {
                result.add(c);
            }
        }
        return result;
    }
}
