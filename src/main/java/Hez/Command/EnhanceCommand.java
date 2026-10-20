package Hez.Command;

import Hez.Enhance.EnhanceDisplayListener;
import Hez.Enhance.EnhanceManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class EnhanceCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }

        // 1. 인자가 없는 경우: 손에 든 아이템 1회 강화 시도
        if (args.length == 0) {
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held.getType() == Material.AIR || !EnhanceManager.isEnhanceable(held)) {
                player.sendMessage("§c강화할 수 있는 장비(검, 도끼, 곡괭이, 활, 삼지창, 철퇴)를 손에 들어주세요.");
                return true;
            }

            EnhanceManager.enhance(player, held);
            return true;
        }

        // 2. /강화 생성 (enhance 태그를 가진 ItemDisplay 스폰)
        if (args[0].equalsIgnoreCase("생성") || args[0].equalsIgnoreCase("spawn")) {
            Location loc = player.getLocation().add(0, 0.5, 0);
            ItemDisplay display = player.getWorld().spawn(loc, ItemDisplay.class, d -> {
                d.addScoreboardTag(EnhanceDisplayListener.ENHANCE_TAG);
                d.setItemStack(new ItemStack(Material.AIR));
                d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                EnhanceDisplayListener.applyEnhanceTransformation(d);
            });

            player.sendMessage("§a[강화대] §f현재 위치에 'enhance' 태그를 가진 강화대 ItemDisplay를 생성했습니다!");
            player.sendMessage("§7(강화 가능한 장비를 들고 우클릭하면 거치/교환할 수 있습니다)");
            return true;
        }

        // 3. /강화 지급 <장비종류> <레벨>
        if (args[0].equalsIgnoreCase("지급") || args[0].equalsIgnoreCase("give")) {
            if (args.length < 3) {
                player.sendMessage("§e사용법: /" + label + " 지급 <검|도끼|곡괭이|창|활|삼지창|철퇴> <1~10>");
                return true;
            }

            String type = args[1].toLowerCase();
            int level;
            try {
                level = Integer.parseInt(args[2]);
                if (level < 1 || level > EnhanceManager.MAX_LEVEL) {
                    player.sendMessage("§c강화 단계는 1부터 " + EnhanceManager.MAX_LEVEL + "까지만 가능합니다.");
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage("§c올바른 숫자를 입력해주세요.");
                return true;
            }

            ItemStack item = switch (type) {
                case "검", "sword" -> EnhanceManager.createEnhancedSword(Material.NETHERITE_SWORD, level);
                case "도끼", "axe" -> EnhanceManager.createEnhancedAxe(Material.NETHERITE_AXE, level);
                case "곡괭이", "pickaxe" -> EnhanceManager.createEnhancedPickaxe(Material.NETHERITE_PICKAXE, level);
                case "창", "spear" -> EnhanceManager.createEnhancedSpear(level);
                case "활", "bow" -> EnhanceManager.createEnhancedBow(Material.BOW, level);
                case "삼지창", "trident" -> EnhanceManager.createEnhancedTrident(level);
                case "철퇴", "mace" -> EnhanceManager.createEnhancedMace(level);
                default -> null;
            };

            if (item == null) {
                player.sendMessage("§c유효하지 않은 장비 종류입니다. (검, 도끼, 곡괭이, 창, 활, 삼지창, 철퇴)");
                return true;
            }

            player.getInventory().addItem(item);
            player.sendMessage("§a[강화] §f+" + level + "강 " + type + " 아이템을 지급받았습니다!");
            return true;
        }

        player.sendMessage("§e사용법:");
        player.sendMessage("§7- §f/" + label + " §8: 손에 든 장비 강화 시도");
        player.sendMessage("§7- §f/" + label + " 생성 §8: 현재 위치에 강화대 ItemDisplay 생성");
        player.sendMessage("§7- §f/" + label + " 지급 <종류> <1~10> §8: 특정 강화 장비 즉시 지급");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = List.of("지급", "생성");
            for (String s : subs) {
                if (s.startsWith(args[0])) completions.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("지급")) {
            List<String> types = List.of("검", "도끼", "곡괭이", "창", "활", "삼지창", "철퇴");
            for (String t : types) {
                if (t.startsWith(args[1])) completions.add(t);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("지급")) {
            for (int i = 1; i <= EnhanceManager.MAX_LEVEL; i++) {
                completions.add(String.valueOf(i));
            }
        }
        return completions;
    }
}
