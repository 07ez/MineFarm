package Hez.Bank;

import Hez.Casino.CasinoMoneyUtil;
import Hez.MineFarm.Main;
import Hez.Money.MoneyManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ATMManager {

    public enum Mode {
        DEPOSIT,
        WITHDRAW
    }

    public record ATMState(UUID uuid, Mode mode, long expiresAt) {
        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    private static final Map<UUID, ATMState> activeStates = new ConcurrentHashMap<>();
    private static final long TIMEOUT_MS = 30_000L; // 30초 대기

    public static boolean hasState(UUID uuid) {
        ATMState state = activeStates.get(uuid);
        if (state == null) return false;
        if (state.isExpired()) {
            activeStates.remove(uuid);
            return false;
        }
        return true;
    }

    public static ATMState getState(UUID uuid) {
        ATMState state = activeStates.get(uuid);
        if (state != null && state.isExpired()) {
            activeStates.remove(uuid);
            return null;
        }
        return state;
    }

    public static void clearState(UUID uuid) {
        activeStates.remove(uuid);
    }

    /**
     * ATM 상호작용 시 클릭 가능한 [입금] [출금] 안내 메뉴 출력
     */
    public static void openATMMenu(Player player) {
        UUID uuid = player.getUniqueId();
        clearState(uuid);

        long balance = MoneyManager.getMoney(uuid);
        long cash = getPlayerTotalCash(player);

        Component divider = Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD);
        Component title = Component.text("         🏦 [ MineFarm ATM 자동화기기 ]         ", NamedTextColor.YELLOW, TextDecoration.BOLD);

        Component infoBalance = Component.text(" ▪ 계좌 잔액: ", NamedTextColor.GRAY)
                .append(Component.text(String.format("%,d원", balance), NamedTextColor.YELLOW, TextDecoration.BOLD));
        Component infoCash = Component.text(" ▪ 소지 지폐: ", NamedTextColor.GRAY)
                .append(Component.text(String.format("%,d원", cash), NamedTextColor.GREEN, TextDecoration.BOLD));

        Component selectPrompt = Component.text("이용하실 업무를 선택해주세요:", NamedTextColor.WHITE);

        // [입금] 클릭 버튼
        Component depositBtn = Component.text(" [ 입금 (Deposit) ] ", NamedTextColor.GREEN, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/atm deposit"))
                .hoverEvent(HoverEvent.showText(Component.text("§a클릭하여 실물 지폐를 계좌로 입금합니다.\n§7(소지 지폐를 계좌 잔액으로 전환)")));

        // [출금] 클릭 버튼
        Component withdrawBtn = Component.text(" [ 출금 (Withdraw) ] ", NamedTextColor.AQUA, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/atm withdraw"))
                .hoverEvent(HoverEvent.showText(Component.text("§b클릭하여 계좌 잔액을 실물 지폐로 출금합니다.\n§7(계좌 잔액을 지폐 아이템으로 지급)")));

        Component buttonLine = Component.text("   ")
                .append(depositBtn)
                .append(Component.text("     "))
                .append(withdrawBtn);

        player.sendMessage(Component.empty());
        player.sendMessage(divider);
        player.sendMessage(title);
        player.sendMessage(Component.empty());
        player.sendMessage(infoBalance);
        player.sendMessage(infoCash);
        player.sendMessage(Component.empty());
        player.sendMessage(selectPrompt);
        player.sendMessage(buttonLine);
        player.sendMessage(divider);

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
    }

    /**
     * 입금 모드 진입
     */
    public static void startDeposit(Player player) {
        UUID uuid = player.getUniqueId();
        long cash = getPlayerTotalCash(player);

        if (cash <= 0) {
            player.sendMessage(Component.text("§c[ATM] 소지하고 계신 실물 지폐가 없습니다."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            clearState(uuid);
            return;
        }

        activeStates.put(uuid, new ATMState(uuid, Mode.DEPOSIT, System.currentTimeMillis() + TIMEOUT_MS));

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("§a━━━━━━━━━━━━━ [ ATM 입금 신청 ] ━━━━━━━━━━━━━"));
        player.sendMessage(Component.text("§a▪ 입금하실 금액을 채팅창에 숫자로 적어주세요."));
        player.sendMessage(Component.text("§7  - 소지 지폐 총액: §e" + String.format("%,d원", cash)));
        player.sendMessage(Component.text("§7  - §f'전액'§7 입력 시 소지 지폐를 모두 입금합니다."));
        player.sendMessage(Component.text("§7  - §c'취소'§7 입력 시 업무를 취소합니다. (30초 제한)"));
        player.sendMessage(Component.text("§a━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }

    /**
     * 출금 모드 진입
     */
    public static void startWithdraw(Player player) {
        UUID uuid = player.getUniqueId();
        long balance = MoneyManager.getMoney(uuid);

        if (balance < 100) {
            player.sendMessage(Component.text("§c[ATM] 출금 가능한 잔액이 부족합니다. (최소 100원 이상 보유 필요)"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            clearState(uuid);
            return;
        }

        activeStates.put(uuid, new ATMState(uuid, Mode.WITHDRAW, System.currentTimeMillis() + TIMEOUT_MS));

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("§b━━━━━━━━━━━━━ [ ATM 출금 신청 ] ━━━━━━━━━━━━━"));
        player.sendMessage(Component.text("§b▪ 출금하실 금액을 채팅창에 숫자로 적어주세요."));
        player.sendMessage(Component.text("§7  - 현재 계좌 잔액: §e" + String.format("%,d원", balance)));
        player.sendMessage(Component.text("§7  - §f'전액'§7 입력 시 계좌 전액을 출금합니다."));
        player.sendMessage(Component.text("§7  - §c'취소'§7 입력 시 업무를 취소합니다. (30초 제한)"));
        player.sendMessage(Component.text("§b━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }

    /**
     * 채팅 입력 처리
     * @return true이면 ATM이 입력을 소비함 (일반 채팅 브로드캐스트 차단)
     */
    public static boolean handleChatInput(Player player, String message) {
        UUID uuid = player.getUniqueId();
        ATMState state = getState(uuid);
        if (state == null) return false;

        String input = message.trim();

        // 취소 명령어
        if (input.equalsIgnoreCase("취소") || input.equalsIgnoreCase("cancel") || input.equalsIgnoreCase("c")) {
            clearState(uuid);
            player.sendMessage(Component.text("§c[ATM] 이용이 취소되었습니다."));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
            return true;
        }

        long amount;
        if (input.equalsIgnoreCase("전액") || input.equalsIgnoreCase("all")) {
            if (state.mode == Mode.DEPOSIT) {
                amount = getPlayerTotalCash(player);
            } else {
                amount = MoneyManager.getMoney(uuid);
            }
        } else {
            // 쉼표 및 "원" 문자 제거 후 파싱
            String sanitized = input.replace(",", "").replace("원", "").trim();
            try {
                amount = Long.parseLong(sanitized);
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("§c[ATM] 올바른 숫자 금액을 입력해주세요. (취소하려면 '취소' 입력)"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return true;
            }
        }

        if (amount <= 0) {
            player.sendMessage(Component.text("§c[ATM] 0원 이상의 금액을 입력해주세요."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        if (amount % 100 != 0) {
            player.sendMessage(Component.text("§c[ATM] 입출금은 지폐 최소 단위인 100원 단위로만 가능합니다."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        if (amount > Integer.MAX_VALUE) {
            player.sendMessage(Component.text("§c[ATM] 1회 최대 처리 한도(21억 원)를 초과하였습니다."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        if (state.mode == Mode.DEPOSIT) {
            executeDeposit(player, (int) amount);
        } else {
            executeWithdraw(player, (int) amount);
        }

        return true;
    }

    /**
     * 입금 실행
     */
    private static void executeDeposit(Player player, int targetAmount) {
        UUID uuid = player.getUniqueId();
        long totalCash = getPlayerTotalCash(player);

        if (targetAmount > totalCash) {
            player.sendMessage(Component.text("§c[ATM] 소지하고 계신 지폐 총액(" + String.format("%,d원", totalCash) + ")보다 큰 금액을 입금할 수 없습니다."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // 지폐 권종별 소지 수량 집계 (100000, 50000, 10000, 1000, 100)
        Map<Integer, Integer> cashCounts = getPlayerCashCounts(player);

        // targetAmount를 소지한 지폐로 정확히 조합할 수 있는지 계산
        Map<Integer, Integer> toDeduct = findExactCombination(cashCounts, targetAmount);
        if (toDeduct == null) {
            player.sendMessage(Component.text("§c[ATM] 소지하고 계신 지폐 조합으로 " + String.format("%,d원", targetAmount) + "을(를) 정확히 맞출 수 없습니다."));
            player.sendMessage(Component.text("§7  (소지 지폐: " + formatCashCounts(cashCounts) + ")"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // 인벤토리에서 지폐 차감
        deductCashFromInventory(player, toDeduct);

        // 계좌 잔액 추가
        MoneyManager.addMoney(uuid, targetAmount);
        int newBalance = MoneyManager.getMoney(uuid);

        clearState(uuid);

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("§a[ATM] 성공적으로 §e" + String.format("%,d원", targetAmount) + "§a을(를) 입금하였습니다."));
        player.sendMessage(Component.text("§7  - 현재 계좌 잔액: §f" + String.format("%,d원", newBalance)));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
    }

    /**
     * 출금 실행
     */
    private static void executeWithdraw(Player player, int targetAmount) {
        UUID uuid = player.getUniqueId();
        int balance = MoneyManager.getMoney(uuid);

        if (targetAmount > balance) {
            player.sendMessage(Component.text("§c[ATM] 계좌 잔액이 부족합니다! (요청: " + String.format("%,d원", targetAmount)
                    + " / 계좌 잔액: " + String.format("%,d원", balance) + ")"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // 환전할 지폐 아이템 목록 생성
        List<ItemStack> items = CasinoMoneyUtil.convertMoneyToItems(targetAmount);

        // 인벤토리 수용 가능 여부 검사
        if (!canFitItems(player, items)) {
            player.sendMessage(Component.text("§c[ATM] 인벤토리 공간이 부족하여 출금할 수 없습니다!"));
            player.sendMessage(Component.text("§7  (인벤토리를 비운 후 다시 시도해주세요.)"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // 계좌 잔액 차감
        if (!MoneyManager.removeMoney(uuid, targetAmount)) {
            player.sendMessage(Component.text("§c[ATM] 잔액 처리 중 오류가 발생하였습니다."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // 아이템 지급
        for (ItemStack item : items) {
            player.getInventory().addItem(item);
        }
        player.updateInventory();

        int newBalance = MoneyManager.getMoney(uuid);
        clearState(uuid);

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("§b[ATM] 성공적으로 §e" + String.format("%,d원", targetAmount) + "§b을(를) 출금하였습니다."));
        player.sendMessage(Component.text("§7  - 현재 계좌 잔액: §f" + String.format("%,d원", newBalance)));
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1.0f, 1.2f);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.5f);
    }

    /**
     * 플레이어 인벤토리 내 실물 지폐 총 가치 합산
     */
    public static long getPlayerTotalCash(Player player) {
        long total = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            int val = CasinoMoneyUtil.getMoneyFromItem(item);
            if (val > 0) {
                total += (long) val * item.getAmount();
            }
        }
        return total;
    }

    /**
     * 플레이어의 권종별 보유 수량 집계 (100, 1000, 10000, 50000, 100000)
     */
    private static Map<Integer, Integer> getPlayerCashCounts(Player player) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int v : new int[]{100000, 50000, 10000, 1000, 100}) {
            map.put(v, 0);
        }
        for (ItemStack item : player.getInventory().getStorageContents()) {
            int val = CasinoMoneyUtil.getMoneyFromItem(item);
            if (val > 0) {
                map.put(val, map.getOrDefault(val, 0) + item.getAmount());
            }
        }
        return map;
    }

    /**
     * 보유 지폐로 targetAmount를 정확히 조합할 수 있는지 탐욕법/백트래킹 계산
     */
    private static Map<Integer, Integer> findExactCombination(Map<Integer, Integer> available, int target) {
        int[] denominations = {100000, 50000, 10000, 1000, 100};
        Map<Integer, Integer> result = new LinkedHashMap<>();
        int remaining = target;

        for (int denom : denominations) {
            int countAvailable = available.getOrDefault(denom, 0);
            if (countAvailable > 0 && remaining >= denom) {
                int take = Math.min(countAvailable, remaining / denom);
                if (take > 0) {
                    result.put(denom, take);
                    remaining -= take * denom;
                }
            }
        }

        if (remaining == 0) {
            return result;
        }
        return null;
    }

    /**
     * 인벤토리에서 지정된 지폐들을 차감
     */
    private static void deductCashFromInventory(Player player, Map<Integer, Integer> toDeduct) {
        Map<Integer, Integer> needed = new HashMap<>(toDeduct);
        ItemStack[] storage = player.getInventory().getStorageContents();

        for (int i = 0; i < storage.length; i++) {
            ItemStack item = storage[i];
            if (item == null) continue;

            int val = CasinoMoneyUtil.getMoneyFromItem(item);
            if (val > 0 && needed.containsKey(val)) {
                int need = needed.get(val);
                if (need <= 0) continue;

                int amount = item.getAmount();
                if (amount <= need) {
                    storage[i] = null;
                    needed.put(val, need - amount);
                } else {
                    item.setAmount(amount - need);
                    needed.put(val, 0);
                }
            }
        }

        player.getInventory().setStorageContents(storage);
        player.updateInventory();
    }

    /**
     * 인벤토리에 아이템들이 다 들어갈 수 있는지 시뮬레이션
     */
    private static boolean canFitItems(Player player, List<ItemStack> itemsToAdd) {
        ItemStack[] storage = player.getInventory().getStorageContents();
        // 시뮬레이션용 인벤토리 복제
        ItemStack[] sim = new ItemStack[storage.length];
        for (int i = 0; i < storage.length; i++) {
            if (storage[i] != null) {
                sim[i] = storage[i].clone();
            }
        }

        for (ItemStack toAdd : itemsToAdd) {
            int remaining = toAdd.getAmount();
            int maxStack = toAdd.getMaxStackSize();

            // 1. 기존 같은 아이템 스택에 추가
            for (ItemStack slot : sim) {
                if (slot != null && slot.isSimilar(toAdd)) {
                    int space = maxStack - slot.getAmount();
                    if (space > 0) {
                        int add = Math.min(space, remaining);
                        slot.setAmount(slot.getAmount() + add);
                        remaining -= add;
                        if (remaining <= 0) break;
                    }
                }
            }

            // 2. 빈 슬롯에 추가
            if (remaining > 0) {
                for (int i = 0; i < sim.length; i++) {
                    if (sim[i] == null || sim[i].getType().isAir()) {
                        int add = Math.min(maxStack, remaining);
                        sim[i] = toAdd.clone();
                        sim[i].setAmount(add);
                        remaining -= add;
                        if (remaining <= 0) break;
                    }
                }
            }

            if (remaining > 0) {
                return false; // 다 못 넣음
            }
        }

        return true;
    }

    private static String formatCashCounts(Map<Integer, Integer> counts) {
        List<String> parts = new ArrayList<>();
        for (int v : new int[]{100000, 50000, 10000, 1000, 100}) {
            int c = counts.getOrDefault(v, 0);
            if (c > 0) {
                parts.add(String.format("%,d원: %d장", v, c));
            }
        }
        return parts.isEmpty() ? "없음" : String.join(", ", parts);
    }
}
