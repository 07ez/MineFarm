package Hez.Command;

import Hez.CustomItem.Fish.FishData;
import Hez.CustomItem.Fish.FishManager;
import Hez.CustomItem.Fish.FishRarity;
import Hez.CustomItem.Fish.FishingMinigame;
import Hez.MineFarm.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class FishCommand implements CommandExecutor {

    private final Main plugin;

    public FishCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }

        if (!player.isOp()) {
            player.sendMessage("§c권한이 없습니다.");
            return true;
        }

        // 사용법: /fish catch <common|rare|epic|legend>
        if (args.length < 2 || !args[0].equalsIgnoreCase("catch")) {
            player.sendMessage("§e[사용법] §f/fish catch <common|rare|epic|legend>");
            return true;
        }

        String rarityArg = args[1].toUpperCase();
        FishRarity targetRarity;
        try {
            targetRarity = FishRarity.valueOf(rarityArg);
        } catch (IllegalArgumentException e) {
            player.sendMessage("§c잘못된 희귀도입니다! (common, rare, epic, legend)");
            return true;
        }

        FishManager fishManager = plugin.getFishManager();
        List<FishData> matchingFish = new ArrayList<>();
        for (FishData fish : fishManager.fishByKey.values()) {
            if (fish.rarity() == targetRarity) {
                matchingFish.add(fish);
            }
        }

        ItemStack sampleReward;
        if (!matchingFish.isEmpty()) {
            FishData sample = matchingFish.get(ThreadLocalRandom.current().nextInt(matchingFish.size()));
            sampleReward = sample.createItemStack();
        } else {
            sampleReward = new ItemStack(org.bukkit.Material.COD);
        }

        player.sendMessage(String.format("§a[낚시 테스트] §f%s 등급 (제한시간: %.1f초, 목표: %d회) 미니게임을 시작합니다!",
                targetRarity.name(), targetRarity.getTimeLimitSeconds(), targetRarity.getTargetHits()));

        FishingMinigame.start(player, sampleReward, targetRarity);
        return true;
    }
}
