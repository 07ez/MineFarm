package Hez.Casino.Holdem;

import Hez.Casino.CasinoMenu;
import Hez.Casino.CasinoMoneyUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HoldemGameSession {

    public enum State {
        WAITING_FOR_BET, // 돈 넣기 전
        PREFLOP,         // 내 카드 2장, 상대 2장 뒷면
        FLOP,            // 공유 카드 3장 오픈
        TURN,            // 공유 카드 4장 오픈
        RIVER,           // 공유 카드 5장 오픈
        SHOWDOWN,        // 쇼다운 (승패 판정)
        FINISHED         // 종료
    }

    private final CasinoMenu menu;
    private final Player player;
    private final HoldemDeck deck = new HoldemDeck();

    private State state = State.WAITING_FOR_BET;

    private final List<HoldemCard> playerCards = new ArrayList<>();
    private final List<HoldemCard> opponentCards = new ArrayList<>();
    private final List<HoldemCard> communityCards = new ArrayList<>();

    private long totalPot = 0;       // 테이블 전체 누적 판돈 (플레이어 + 상대 콜)
    private long playerBetTotal = 0;  // 플레이어가 투입한 총 금액
    private String lastBetModel = "1010"; // 마지막 투입 지폐 CMD

    public HoldemGameSession(CasinoMenu menu, Player player) {
        this.menu = menu;
        this.player = player;
    }

    public State getState() {
        return state;
    }

    public long getTotalPot() {
        return totalPot;
    }

    public long getPlayerBetTotal() {
        return playerBetTotal;
    }

    public boolean isGameActive() {
        return state == State.PREFLOP || state == State.FLOP || state == State.TURN || state == State.RIVER;
    }

    /**
     * 돈 넣는 자리에 돈을 올렸을 때 게임 시작 (프리플랍)
     */
    public void startGame(ItemStack betItem) {
        String cmd = getCustomModelData(betItem);
        if (cmd == null) cmd = "1010";
        this.lastBetModel = cmd;

        int unitVal = CasinoMoneyUtil.getMoneyValue(cmd);
        long initialBet = (long) unitVal * betItem.getAmount();

        this.playerBetTotal = initialBet;
        // 상대도 동일 금액 콜하여 판돈에 합산
        this.totalPot = initialBet * 2;

        // 덱 리셋 및 카드 분배
        deck.reset();
        playerCards.clear();
        opponentCards.clear();
        communityCards.clear();

        // 1. 내 카드 2장
        playerCards.add(deck.draw());
        playerCards.add(deck.draw());

        // 2. 상대 카드 2장
        opponentCards.add(deck.draw());
        opponentCards.add(deck.draw());

        // 3. 공유 카드 5장 준비
        for (int i = 0; i < 5; i++) {
            communityCards.add(deck.draw());
        }

        // GUI 상에 카드 배치
        // 상대 카드 (1줄 4, 5번째 -> 슬롯 3, 4): 뒷면 배치
        menu.getInventory().setItem(3, HoldemCard.createBackCard());
        menu.getInventory().setItem(4, HoldemCard.createBackCard());

        // 내 카드 (5줄 5, 6번째 -> 슬롯 40, 41): 앞면 배치
        menu.getInventory().setItem(40, playerCards.get(0).createItemStack(true));
        menu.getInventory().setItem(41, playerCards.get(1).createItemStack(true));

        // 공유 카드 슬롯 (3줄 3~7번째 -> 슬롯 20~24) 비우기
        for (int slot = 20; slot <= 24; slot++) {
            menu.getInventory().setItem(slot, null);
        }

        // 슬롯 1 판돈 표시 아이템 업데이트
        updatePotDisplay();

        this.state = State.PREFLOP;

        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.0f);
        player.sendMessage("§e[텍사스 홀덤] §f게임이 시작되었습니다! §7(내 카드 2장 지급 완료)");
        player.sendMessage("§c[상대] §f상대가 콜했습니다! (내 차례)");
    }

    /**
     * 레이즈 (배팅 증액)
     * isButton: 레이즈 종이 버튼 클릭 여부
     */
    public void raise(boolean isButton, ItemStack extraItem) {
        if (!isGameActive()) return;

        long raiseAmount = 0;

        if (isButton) {
            // 종이 버튼 클릭: 현재 내 배팅금(playerBetTotal)만큼 추가 레이즈 시도
            long targetRaise = playerBetTotal > 0 ? playerBetTotal : CasinoMoneyUtil.getMoneyValue(lastBetModel);
            int unitVal = CasinoMoneyUtil.getMoneyValue(lastBetModel);
            int neededCount = (int) Math.ceil((double) targetRaise / unitVal);

            // 인벤토리에서 동일 지폐 검색 및 차감
            int foundCount = countItemInInventory(lastBetModel);
            if (foundCount <= 0) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.sendMessage("§c[텍사스 홀덤] 레이즈에 필요한 지폐(" + CasinoMoneyUtil.getMoneyName(lastBetModel) + ")가 부족합니다!");
                return;
            }

            int takeCount = Math.min(neededCount, foundCount);
            deductItemFromInventory(lastBetModel, takeCount);
            raiseAmount = (long) takeCount * unitVal;

            if (takeCount < neededCount) {
                player.sendMessage("§e[텍사스 홀덤] 보유한 지폐로 전부 올인 레이즈했습니다!");
            }
        } else if (extraItem != null) {
            // 슬롯 1에 직접 지폐를 얹은 경우
            String cmd = getCustomModelData(extraItem);
            if (cmd != null) {
                this.lastBetModel = cmd;
                int unitVal = CasinoMoneyUtil.getMoneyValue(cmd);
                raiseAmount = (long) unitVal * extraItem.getAmount();
            }
        }

        if (raiseAmount <= 0) return;

        this.playerBetTotal += raiseAmount;
        // 상대도 동일 금액 콜
        this.totalPot += (raiseAmount * 2);

        updatePotDisplay();

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
        player.sendMessage("§6[텍사스 홀덤] §f" + String.format("%,d", raiseAmount) + "원을 레이즈했습니다!");
        player.sendMessage("§c[상대] §f상대가 콜했습니다!");

        // 상대가 콜했으므로 다음 턴(다음 스트리트: 플랍, 턴, 리버, 쇼다운)으로 진행!
        nextStreet();
    }

    /**
     * 콜 (현재 상태 유지하고 다음 스트리트로 진행)
     */
    public void call() {
        if (!isGameActive()) return;

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
        nextStreet();
    }

    /**
     * 다음 스트리트 진행 (프리플랍 -> 플랍 -> 턴 -> 리버 -> 쇼다운)
     */
    private void nextStreet() {
        switch (state) {
            case PREFLOP -> {
                // 플랍: 공유 카드 3장 오픈 (슬롯 20, 21, 22)
                menu.getInventory().setItem(20, communityCards.get(0).createItemStack(true));
                menu.getInventory().setItem(21, communityCards.get(1).createItemStack(true));
                menu.getInventory().setItem(22, communityCards.get(2).createItemStack(true));

                state = State.FLOP;
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 1.2f);
                player.sendMessage("§6[텍사스 홀덤] §e플랍(Flop) §f카드 3장이 공개되었습니다!");
                player.sendMessage("§c[상대] §f상대가 콜했습니다! (내 차례)");
            }
            case FLOP -> {
                // 턴: 공유 카드 4번째 오픈 (슬롯 23)
                menu.getInventory().setItem(23, communityCards.get(3).createItemStack(true));

                state = State.TURN;
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 1.2f);
                player.sendMessage("§6[텍사스 홀덤] §e턴(Turn) §f카드 1장이 추가 공개되었습니다!");
                player.sendMessage("§c[상대] §f상대가 콜했습니다! (내 차례)");
            }
            case TURN -> {
                // 리버: 공유 카드 5번째 오픈 (슬롯 24)
                menu.getInventory().setItem(24, communityCards.get(4).createItemStack(true));

                state = State.RIVER;
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 1.2f);
                player.sendMessage("§6[텍사스 홀덤] §e리버(River) §f마지막 카드가 공개되었습니다!");
                player.sendMessage("§c[상대] §f상대가 콜했습니다! (내 차례)");
            }
            case RIVER -> {
                // 쇼다운 (결과 발표)
                showdown();
            }
            default -> {
            }
        }
    }

    /**
     * 쇼다운: 상대 카드 오픈 및 텍사스 홀덤 족보 판정
     */
    public void showdown() {
        state = State.SHOWDOWN;

        // 상대 카드 2장 앞면 공개
        menu.getInventory().setItem(3, opponentCards.get(0).createItemStack(true));
        menu.getInventory().setItem(4, opponentCards.get(1).createItemStack(true));

        // 플레이어 7장 (핸드 2장 + 커뮤니티 5장)
        List<HoldemCard> player7 = new ArrayList<>(playerCards);
        player7.addAll(communityCards);

        // 상대 7장 (핸드 2장 + 커뮤니티 5장)
        List<HoldemCard> opponent7 = new ArrayList<>(opponentCards);
        opponent7.addAll(communityCards);

        HoldemHandEvaluator.HandScore playerScore = HoldemHandEvaluator.evaluate7(player7);
        HoldemHandEvaluator.HandScore opponentScore = HoldemHandEvaluator.evaluate7(opponent7);

        int cmp = playerScore.compareTo(opponentScore);

        if (cmp > 0) {
            // 플레이어 승리: 판돈 전체를 지폐로 환전하여 지급
            List<ItemStack> rewards = CasinoMoneyUtil.convertMoneyToItems(totalPot);
            for (ItemStack reward : rewards) {
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(reward);
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            player.sendMessage("§6§l[홀덤 쇼다운] §a§l당신의 승리!");
            player.sendMessage("§f내 족보: §a" + playerScore.getDescription() + " §7| §f상대 족보: §c" + opponentScore.getDescription());
            player.sendMessage("§6획득한 판돈: §e" + String.format("%,d", totalPot) + "원 지급 완료!");
        } else if (cmp < 0) {
            // 플레이어 패배: 판돈 소멸
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
            player.sendMessage("§6§l[홀덤 쇼다운] §c§l상대의 승리 (패배)");
            player.sendMessage("§f내 족보: §7" + playerScore.getDescription() + " §7| §f상대 족보: §e" + opponentScore.getDescription());
            player.sendMessage("§c판돈 " + String.format("%,d", totalPot) + "원이 소멸되었습니다.");
        } else {
            // 무승부 (타이): 원금 환전 반환
            List<ItemStack> refund = CasinoMoneyUtil.convertMoneyToItems(playerBetTotal);
            for (ItemStack item : refund) {
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }

            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            player.sendMessage("§6§l[홀덤 쇼다운] §e§l무승부 (원금 반환)");
            player.sendMessage("§f동일 족보: §e" + playerScore.getDescription());
            player.sendMessage("§e배팅 원금 " + String.format("%,d", playerBetTotal) + "원이 반환되었습니다.");
        }

        this.state = State.FINISHED;
        // 슬롯 1 판돈 종이 제거 (다음 게임을 위해 빈 슬롯화)
        menu.getInventory().setItem(1, null);
    }

    /**
     * 폴드 (기권)
     */
    public void fold() {
        if (!isGameActive()) return;

        this.state = State.FINISHED;
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
        player.sendMessage("§c[텍사스 홀덤] 기권(폴드)하셨습니다. 판돈 " + String.format("%,d", totalPot) + "원이 모두 소멸되었습니다.");

        // 판돈 종이 및 상대 카드 제거
        menu.getInventory().setItem(1, null);
        menu.getInventory().setItem(3, null);
        menu.getInventory().setItem(4, null);
    }

    /**
     * 슬롯 1에 '현재 판돈: 00원' 종이 아이템 거치
     */
    public void updatePotDisplay() {
        ItemStack potItem = new ItemStack(Material.PAPER);
        ItemMeta meta = potItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e§l현재 판돈");
            meta.setLore(List.of(
                    "§f현재 판돈: §6" + String.format("%,d", totalPot) + "원",
                    "§7내가 건 돈: §e" + String.format("%,d", playerBetTotal) + "원",
                    "§7상대 콜 금액: §e" + String.format("%,d", (totalPot - playerBetTotal)) + "원"
            ));
            potItem.setItemMeta(meta);
        }
        menu.getInventory().setItem(1, potItem);
    }

    private int countItemInInventory(String cmd) {
        int count = 0;
        for (ItemStack is : player.getInventory().getContents()) {
            if (is != null && is.getType() == Material.PAPER && is.hasItemMeta()) {
                String isCmd = getCustomModelData(is);
                if (cmd.equals(isCmd)) {
                    count += is.getAmount();
                }
            }
        }
        return count;
    }

    private void deductItemFromInventory(String cmd, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack is = contents[i];
            if (is != null && is.getType() == Material.PAPER && is.hasItemMeta()) {
                String isCmd = getCustomModelData(is);
                if (cmd.equals(isCmd)) {
                    if (is.getAmount() <= remaining) {
                        remaining -= is.getAmount();
                        player.getInventory().setItem(i, null);
                    } else {
                        is.setAmount(is.getAmount() - remaining);
                        remaining = 0;
                        break;
                    }
                }
            }
        }
    }

    private String getCustomModelData(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        try {
            var comp = item.getItemMeta().getCustomModelDataComponent();
            var strings = comp.getStrings();
            if (strings != null && !strings.isEmpty()) {
                return strings.get(0);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
