package Hez.Casino;

import Hez.Casino.Holdem.HoldemGameSession;
import Hez.MineFarm.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class CasinoInteractionListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        // 메인 손 우클릭만 감지 (양손 중복 호출 방지)
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Entity clicked = event.getRightClicked();
        if (clicked == null) {
            return;
        }

        CasinoType matchedType = null;
        for (String tag : clicked.getScoreboardTags()) {
            CasinoType type = CasinoType.fromTag(tag);
            if (type != null) {
                matchedType = type;
                break;
            }
        }

        if (matchedType == null) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        CasinoMenu menu = new CasinoMenu(player, matchedType);
        player.openInventory(menu.getInventory());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof CasinoMenu menu)) {
            return;
        }

        // 룰렛 또는 슬롯머신 회전 중에는 모든 조작 차단
        if (menu.isSpinning() || menu.isSlotSpinning()) {
            event.setCancelled(true);
            return;
        }

        // 1. 더블 클릭 차단
        if (event.getClick() == ClickType.DOUBLE_CLICK || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }

        int rawSlot = event.getRawSlot();
        int moneySlot = menu.getType().getMoneySlot();

        // 2. 플레이어 하단 인벤토리 쉬프트 클릭 차단
        if (event.isShiftClick() && rawSlot >= 54) {
            event.setCancelled(true);
            return;
        }

        // 3. 숫자 키 핫바 스왑 차단
        if (event.getClick() == ClickType.NUMBER_KEY ||
                event.getAction() == InventoryAction.HOTBAR_SWAP ||
                event.getAction().name().contains("HOTBAR")) {
            event.setCancelled(true);
            return;
        }

        // 4. 상단 GUI 영역 (0 ~ 53)
        if (rawSlot < 54) {
            Player player = (Player) event.getWhoClicked();

            // ==========================================
            // 룰렛 (ROULETTE) 조작 버튼: 슬롯 51, 52, 53
            // ==========================================
            if (menu.getType() == CasinoType.ROULETTE && (rawSlot >= 51 && rawSlot <= 53)) {
                event.setCancelled(true);

                if (event.isRightClick()) {
                    if (rawSlot == 51) {
                        menu.toggleColor();
                    } else if (rawSlot == 52) {
                        menu.cycleBundle();
                    } else {
                        menu.cycleNumber(event.isShiftClick());
                    }
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
                } else if (event.isLeftClick()) {
                    playRouletteGame(player, menu, rawSlot);
                }
                return;
            }

            // ==========================================
            // 텍사스 홀덤 (HOLDEM) 조작 버튼: 슬롯 34, 35(레이즈), 44(콜), 52, 53(폴드)
            // ==========================================
            if (menu.getType() == CasinoType.HOLDEM && (rawSlot == 34 || rawSlot == 35 || rawSlot == 44 || rawSlot == 52 || rawSlot == 53)) {
                event.setCancelled(true);
                HoldemGameSession session = menu.getHoldemSession();
                if (session == null || !session.isGameActive()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage("§c[텍사스 홀덤] 먼저 1번 슬롯에 돈을 넣어 게임을 시작해야 합니다!");
                    return;
                }

                if (rawSlot == 34 || rawSlot == 35) {
                    session.raise(true, null); // 레이즈
                } else if (rawSlot == 44) {
                    session.call(); // 콜
                } else {
                    session.fold(); // 폴드
                }
                return;
            }

            // ==========================================
            // 슬롯머신 (SLOT_MACHINE) 스핀 레버: 슬롯 23
            // ==========================================
            if (menu.getType() == CasinoType.SLOT_MACHINE && rawSlot == 23) {
                event.setCancelled(true);
                playSlotMachineGame(player, menu);
                return;
            }

            // ==========================================
            // 돈 넣는 자리 조작 제어
            // ==========================================
            if (rawSlot != moneySlot) {
                event.setCancelled(true);
                return;
            }

            // 텍사스 홀덤 전용 슬롯 1 (돈 넣는 자리)
            if (menu.getType() == CasinoType.HOLDEM) {
                event.setCancelled(true);
                HoldemGameSession session = menu.getHoldemSession();
                if (session == null) return;

                ItemStack cursorItem = event.getCursor();
                if (!isMoneyItem(cursorItem)) {
                    return;
                }

                // 커서의 돈 아이템 가져오기
                int placeAmount = (event.isRightClick()) ? 1 : cursorItem.getAmount();
                ItemStack betItemStack = cursorItem.clone();
                betItemStack.setAmount(placeAmount);

                if (cursorItem.getAmount() <= placeAmount) {
                    event.getView().setCursor(null);
                } else {
                    cursorItem.setAmount(cursorItem.getAmount() - placeAmount);
                    event.getView().setCursor(cursorItem);
                }

                if (!session.isGameActive()) {
                    // 게임 시작
                    session.startGame(betItemStack);
                } else {
                    // 진행 중 추가 레이즈
                    session.raise(false, betItemStack);
                }
                return;
            }

            // 룰렛/슬롯머신 돈 넣는 자리 조작 허용
            ItemStack cursorItem = event.getCursor();
            InventoryAction action = event.getAction();

            switch (action) {
                case PLACE_ALL, PLACE_ONE, PLACE_SOME -> {
                    if (!isMoneyItem(cursorItem)) {
                        event.setCancelled(true);
                    }
                }
                case SWAP_WITH_CURSOR -> {
                    if (!isMoneyItem(cursorItem)) {
                        event.setCancelled(true);
                    }
                }
                case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME -> {
                }
                case NOTHING -> {
                }
                default -> event.setCancelled(true);
            }
        }
    }

    /**
     * 룰렛 게임 시작
     */
    private void playRouletteGame(Player player, CasinoMenu menu, int modeSlot) {
        int moneySlot = menu.getType().getMoneySlot(); // 50번
        ItemStack betItem = menu.getInventory().getItem(moneySlot);

        if (betItem == null || betItem.getType() == Material.AIR || !isMoneyItem(betItem)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage("§c[룰렛] 배팅할 돈(지폐)을 돈 넣는 자리에 먼저 올려두어야 합니다!");
            return;
        }

        int betAmount = betItem.getAmount();
        ItemStack rewardTemplate = betItem.clone();

        menu.getInventory().setItem(moneySlot, null);

        int luckyNumber = ThreadLocalRandom.current().nextInt(1, 37);
        menu.setSpinning(true);

        int startNum = menu.getCurrentWheelNumber();
        int fastSteps = 54 + ((luckyNumber - startNum - 54 - 18) % 36 + 36) % 36;
        int totalSteps = fastSteps + 18;

        new WheelSpinTask(player, menu, modeSlot, luckyNumber, totalSteps, startNum, betAmount, rewardTemplate).start();
    }

    private static class WheelSpinTask implements Runnable {
        private static final int[] SLOWDOWN_TICKS = {2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 5, 6, 7, 8, 9, 10};

        private final Player player;
        private final CasinoMenu menu;
        private final int modeSlot;
        private final int luckyNumber;
        private final int totalSteps;
        private final int betAmount;
        private final ItemStack rewardTemplate;

        private int currentStep = 0;
        private int currentNum;

        public WheelSpinTask(Player player, CasinoMenu menu, int modeSlot, int luckyNumber,
                             int totalSteps, int startNum, int betAmount, ItemStack rewardTemplate) {
            this.player = player;
            this.menu = menu;
            this.modeSlot = modeSlot;
            this.luckyNumber = luckyNumber;
            this.totalSteps = totalSteps;
            this.currentNum = startNum;
            this.betAmount = betAmount;
            this.rewardTemplate = rewardTemplate;
        }

        public void start() {
            run();
        }

        @Override
        public void run() {
            if (!player.isOnline() || !menu.isSpinning()) {
                return;
            }

            int remaining = totalSteps - currentStep;

            if (currentStep > 0) {
                currentNum = (currentNum % 36) + 1;
                menu.setCurrentWheelNumber(currentNum);

                try {
                    player.getOpenInventory().setTitle("§f" + CasinoMenu.OFFSET_UNICODE + CasinoMenu.getWheelUnicode(currentNum));
                } catch (Throwable ignored) {
                }

                float pitch = 2.0f;
                if (remaining < 18) {
                    int slowIndex = 18 - remaining;
                    pitch = Math.max(1.1f, 2.0f - (slowIndex * 0.05f));
                }
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.7f, pitch);
            }

            if (remaining <= 0) {
                player.playSound(player.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 0.5f, 1.6f);

                menu.setSpinning(false);
                menu.setSpinningTask(null);
                menu.setCurrentWheelNumber(luckyNumber);

                finishRouletteGame(player, menu, modeSlot, luckyNumber, betAmount, rewardTemplate);
                return;
            }

            currentStep++;

            long delayTicks = 1L;
            if (remaining <= SLOWDOWN_TICKS.length) {
                int tickIndex = SLOWDOWN_TICKS.length - remaining;
                delayTicks = SLOWDOWN_TICKS[tickIndex];
            }

            BukkitTask nextTask = Bukkit.getScheduler().runTaskLater(Main.getInstance(), this, delayTicks);
            menu.setSpinningTask(nextTask);
        }
    }

    private static void finishRouletteGame(Player player, CasinoMenu menu, int modeSlot,
                                          int luckyNumber, int betAmount, ItemStack rewardTemplate) {
        String luckyColor = (luckyNumber % 2 == 1) ? "검정" : "빨강";
        String luckyBundle = (luckyNumber <= 12) ? "1~12" : (luckyNumber <= 24 ? "13~24" : "25~36");

        boolean isWin = false;
        int multiplier = 0;
        String modeName;
        String myChoice;

        if (modeSlot == 51) {
            modeName = "색맞추기";
            myChoice = menu.getSelectedColor();
            multiplier = 2;
            if (myChoice.equals(luckyColor)) isWin = true;
        } else if (modeSlot == 52) {
            modeName = "묶음 맞추기";
            myChoice = menu.getSelectedBundle();
            multiplier = 3;
            if (myChoice.equals(luckyBundle)) isWin = true;
        } else {
            modeName = "숫자맞추기";
            myChoice = String.valueOf(menu.getSelectedNumber());
            multiplier = 36;
            if (menu.getSelectedNumber() == luckyNumber) isWin = true;
        }

        if (isWin) {
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

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
        }

        String resultMsg = "§6§l[룰렛] §f모드: §e" + modeName +
                " §7| §f선택: §b" + myChoice +
                " §7| §f추첨: §a" + luckyNumber + "번(" + luckyColor + ") §7➔ " +
                (isWin ? "§a§l[당첨! " + multiplier + "배 지급 (+" + (betAmount * multiplier) + "장)]" : "§c§l[낙첨]");

        player.sendMessage(resultMsg);
    }

    /**
     * 슬롯머신 스핀 시작 (배팅금 검사, 차감, 애니메이션 시작)
     */
    private void playSlotMachineGame(Player player, CasinoMenu menu) {
        if (menu.isSlotSpinning()) return;

        int moneySlot = menu.getType().getMoneySlot(); // 48번
        ItemStack betItem = menu.getInventory().getItem(moneySlot);

        if (betItem == null || betItem.getType() == Material.AIR || !isMoneyItem(betItem)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage("§c[슬롯머신] 48번 슬롯에 돈을 넣고 스핀을 돌려야 합니다!");
            return;
        }

        int betAmount = betItem.getAmount();
        ItemStack rewardTemplate = betItem.clone();

        // 배팅금 슬롯에서 즉시 차감 (판돈 소모)
        menu.getInventory().setItem(moneySlot, null);

        menu.setSlotSpinning(true);
        player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0f, 1.2f);

        Hez.Casino.Slot.SlotMachineTask task = new Hez.Casino.Slot.SlotMachineTask(player, menu, betAmount, rewardTemplate);
        menu.setSlotSpinTask(task.start());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof CasinoMenu menu)) {
            return;
        }

        if (menu.isSpinning() || menu.isSlotSpinning()) {
            event.setCancelled(true);
            return;
        }

        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < 54) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof CasinoMenu menu)) {
            return;
        }

        // 1. 룰렛 회전 도중 창 닫힘 -> 몰수
        if (menu.isSpinning()) {
            menu.setSpinning(false);
            if (menu.getSpinningTask() != null) {
                menu.getSpinningTask().cancel();
                menu.setSpinningTask(null);
            }
            Player player = (Player) event.getPlayer();
            player.sendMessage("§c[룰렛] 게임 도중에 창을 닫아 배팅금이 소멸되었습니다!");
            return;
        }

        // 2. 슬롯머신 회전 도중 창 닫힘 -> 몰수
        if (menu.isSlotSpinning()) {
            menu.setSlotSpinning(false);
            if (menu.getSlotSpinTask() != null) {
                menu.getSlotSpinTask().cancel();
                menu.setSlotSpinTask(null);
            }
            Player player = (Player) event.getPlayer();
            player.sendMessage("§c[슬롯머신] 게임 도중에 창을 닫아 배팅금이 소멸되었습니다!");
            return;
        }

        // 3. 텍사스 홀덤 게임 도중 창 닫힘 -> 기권(몰수)
        if (menu.getType() == CasinoType.HOLDEM) {
            HoldemGameSession session = menu.getHoldemSession();
            if (session != null && session.isGameActive()) {
                session.fold();
                Player player = (Player) event.getPlayer();
                player.sendMessage("§c[텍사스 홀덤] 게임 도중에 창을 닫아 기권(폴드) 처리되었습니다.");
                return;
            }
        }

        // 4. 평상시 창을 닫을 때 돈 넣는 자리에 남은 돈 반환 (룰렛/슬롯머신)
        int moneySlot = menu.getType().getMoneySlot();
        ItemStack bettingItem = menu.getInventory().getItem(moneySlot);

        if (bettingItem != null && bettingItem.getType() != Material.AIR && isMoneyItem(bettingItem)) {
            menu.getInventory().setItem(moneySlot, null);

            Player player = (Player) event.getPlayer();
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(bettingItem);

            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
    }

    public static boolean isMoneyItem(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        CustomModelDataComponent cmd = meta.getCustomModelDataComponent();
        List<String> strings = cmd.getStrings();
        if (strings == null || strings.isEmpty()) {
            return false;
        }

        String modelData = strings.get(0);
        return switch (modelData) {
            case "1010", "1011", "1012", "1013", "1014" -> true;
            default -> false;
        };
    }
}
