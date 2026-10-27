package Hez.Casino.Slot;

import Hez.Casino.CasinoMenu;
import Hez.Casino.CasinoMoneyUtil;
import Hez.MineFarm.Main;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

public class SlotMachineTask extends BukkitRunnable {

    private final Player player;
    private final CasinoMenu menu;
    private final int betAmount;
    private final ItemStack rewardTemplate;

    // 최종 목표 심볼 (가중치 확률 모델로 사전 추첨)
    private final SlotSymbol target1 = SlotSymbol.getRandom();
    private final SlotSymbol target2 = SlotSymbol.getRandom();
    private final SlotSymbol target3 = SlotSymbol.getRandom();

    private int ticks = 0;

    // 각 릴의 정지 시점 (틱 단위)
    private static final int STOP_REEL_1 = 24; // 1.2초
    private static final int STOP_REEL_2 = 38; // 1.9초
    private static final int STOP_REEL_3 = 54; // 2.7초

    private boolean reel1Stopped = false;
    private boolean reel2Stopped = false;
    private boolean reel3Stopped = false;

    public SlotMachineTask(Player player, CasinoMenu menu, int betAmount, ItemStack rewardTemplate) {
        this.player = player;
        this.menu = menu;
        this.betAmount = betAmount;
        this.rewardTemplate = rewardTemplate;
    }

    public org.bukkit.scheduler.BukkitTask start() {
        return runTaskTimer(Main.getInstance(), 1L, 2L); // 2틱(0.1초) 주기로 갱신
    }

    @Override
    public void run() {
        if (!player.isOnline() || !menu.isSlotSpinning()) {
            cancel();
            return;
        }

        ticks += 2;

        // 회전 효과음 (노트블록 햇)
        if (ticks < STOP_REEL_3) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.8f);
        }

        // 1번 릴 (슬롯 20)
        if (ticks < STOP_REEL_1) {
            menu.getInventory().setItem(20, SlotSymbol.getRandomVisual().createItemStack());
        } else if (!reel1Stopped) {
            reel1Stopped = true;
            menu.getInventory().setItem(20, target1.createItemStack());
            player.playSound(player.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 0.8f, 1.4f);
        }

        // 2번 릴 (슬롯 21)
        if (ticks < STOP_REEL_2) {
            menu.getInventory().setItem(21, SlotSymbol.getRandomVisual().createItemStack());
        } else if (!reel2Stopped) {
            reel2Stopped = true;
            menu.getInventory().setItem(21, target2.createItemStack());
            player.playSound(player.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 0.8f, 1.4f);
        }

        // 3번 릴 (슬롯 22)
        if (ticks < STOP_REEL_3) {
            menu.getInventory().setItem(22, SlotSymbol.getRandomVisual().createItemStack());
        } else if (!reel3Stopped) {
            reel3Stopped = true;
            menu.getInventory().setItem(22, target3.createItemStack());
            player.playSound(player.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 0.9f, 1.6f);

            // 3개 릴 모두 정지 -> 최종 판정 및 종료
            cancel();
            menu.setSlotSpinning(false);
            menu.setSlotSpinTask(null);

            finishSlotGame();
        }
    }

    private void finishSlotGame() {
        boolean isJackpot = (target1 == target2 && target2 == target3);

        if (isJackpot) {
            int multiplier = target1.getMultiplier();
            int totalReward = betAmount * multiplier;
            int remaining = totalReward;

            while (remaining > 0) {
                int giveCount = Math.min(remaining, 64);
                ItemStack reward = rewardTemplate.clone();
                reward.setAmount(giveCount);

                Map<Integer, ItemStack> leftover = player.getInventory().addItem(reward);
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                remaining -= giveCount;
            }

            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
            player.sendMessage("§6§l[슬롯머신] §a§l당첨 (JACKPOT)! §f[" + target1.getName() + " §f3개 일치] §e" + multiplier + "배 지급 (+" + totalReward + "장)!");
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
            player.sendMessage("§6§l[슬롯머신] §c§l낙첨! §7결과: [" + target1.getName() + "§7 | " + target2.getName() + "§7 | " + target3.getName() + "§7]");
        }
    }
}
