package Hez.Casino;

import Hez.Casino.Holdem.HoldemGameSession;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public class CasinoMenu implements InventoryHolder {

    public static final Key FONT_DEFAULT = Key.key("minecraft", "default");
    public static final String OFFSET_UNICODE = "\uEBBB";

    private final Inventory inventory;
    private final CasinoType type;
    private final Player player;

    // 룰렛(wheel) 전용 선택 상태
    private String selectedColor = "빨강"; // "빨강" or "검정"
    private String selectedBundle = "1~12"; // "1~12", "13~24", "25~36"
    private int selectedNumber = 1; // 1 ~ 36

    // 룰렛 회전 애니메이션 상태
    private int currentWheelNumber = 1; // 1 ~ 36 (초기값 1번: 501)
    private boolean isSpinning = false;
    private BukkitTask spinningTask = null;

    // 텍사스 홀덤 세션
    private HoldemGameSession holdemSession = null;

    // 슬롯머신(slot) 회전 상태
    private boolean isSlotSpinning = false;
    private BukkitTask slotSpinTask = null;

    public CasinoMenu(Player player, CasinoType type) {
        this.player = player;
        this.type = type;

        // 초기 타이틀 생성
        Component title;
        if (type == CasinoType.ROULETTE) {
            title = createWheelTitle(this.currentWheelNumber);
        } else {
            title = Component.text()
                    .append(Component.text(OFFSET_UNICODE + type.getUnicode())
                            .font(FONT_DEFAULT)
                            .color(NamedTextColor.WHITE)
                            .shadowColor(ShadowColor.none()))
                    .build();
        }

        // 9 x 6 (54칸) 대형 상자 생성
        this.inventory = Bukkit.createInventory(this, 54, title);

        if (type == CasinoType.HOLDEM) {
            this.holdemSession = new HoldemGameSession(this, player);
        }

        setupItems();
    }

    /**
     * 룰렛 번호(1~36)에 해당하는 유니코드 글리프 생성 (E501 ~ E536)
     */
    public static String getWheelUnicode(int number) {
        char c = (char) Integer.parseInt("E5" + String.format("%02d", number), 16);
        return String.valueOf(c);
    }

    /**
     * 룰렛 번호에 맞춘 전체 커스텀 GUI 타이틀 컴포넌트 생성
     */
    public static Component createWheelTitle(int number) {
        return Component.text()
                .append(Component.text(OFFSET_UNICODE + getWheelUnicode(number))
                        .font(FONT_DEFAULT)
                        .color(NamedTextColor.WHITE)
                        .shadowColor(ShadowColor.none()))
                .build();
    }

    private void setupItems() {
        switch (type) {
            case ROULETTE -> updateWheelItems();
            case HOLDEM -> {
                // 4줄 8, 9번째 (슬롯 34, 35): 레이즈 (CMD 1002)
                ItemStack raiseItem = createPaperWithModel("§e§l레이즈", "1002",
                        "§7내 배팅금의 2배 레이즈",
                        "§e[클릭 시 배팅]");
                inventory.setItem(34, raiseItem);
                inventory.setItem(35, raiseItem);

                // 5줄 9번째 (슬롯 44): 콜 (CMD 1002)
                ItemStack callItem = createPaperWithModel("§a§l콜", "1002",
                        "§7상대와 동일 금액 유지 및 진행",
                        "§a[클릭 시 진행]");
                inventory.setItem(44, callItem);

                // 6줄 8, 9번째 (슬롯 52, 53): 폴드 (CMD 1002)
                ItemStack foldItem = createPaperWithModel("§c§l폴드", "1002",
                        "§7판돈을 포기하고 기권",
                        "§c[클릭 시 기권]");
                inventory.setItem(52, foldItem);
                inventory.setItem(53, foldItem);
            }
            case SLOT_MACHINE -> {
                // 3줄 3, 4, 5번째 (슬롯 20, 21, 22): 릴 초기 종이
                ItemStack paper = createPaper("§f[릴 대기중]");
                inventory.setItem(20, paper);
                inventory.setItem(21, paper);
                inventory.setItem(22, paper);

                // 3줄 6번째 (슬롯 23): 마지막 종이 바로 다음 칸 스핀 돌리기 레버 버튼
                ItemStack spinLever = new ItemStack(Material.LEVER);
                ItemMeta meta = spinLever.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§e§l스핀 돌리기 (SPIN)");
                    meta.setLore(List.of(
                            "§7돈 넣는 자리(48번)에 돈을 넣고 클릭!",
                            "§e[클릭 시 회전 시작]"
                    ));
                    spinLever.setItemMeta(meta);
                }
                inventory.setItem(23, spinLever);
            }
        }
    }

    /**
     * 룰렛 3대 조작 버튼 아이템 갱신 (슬롯 51, 52, 53)
     */
    public void updateWheelItems() {
        ItemStack colorItem = createPaper("§f색맞추기",
                "§7현재고른 색상: §e" + selectedColor,
                "§7우클릭: 색상 변경",
                "§a좌클릭: 게임시작하기");
        inventory.setItem(51, colorItem);

        ItemStack bundleItem = createPaper("§f묶음 맞추기",
                "§7현재고른 묶음: §e" + selectedBundle,
                "§7우클릭: 묶음 변경",
                "§a좌클릭: 게임시작하기");
        inventory.setItem(52, bundleItem);

        ItemStack numberItem = createPaper("§f숫자맞추기",
                "§7현재고른 숫자: §e" + selectedNumber,
                "§7우클릭: 숫자 변경",
                "§a좌클릭: 게임시작하기");
        inventory.setItem(53, numberItem);
    }

    private ItemStack createPaper(String name, String... lores) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lores != null && lores.length > 0) {
                meta.setLore(List.of(lores));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPaperWithModel(String name, String cmd, String... lores) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lores != null && lores.length > 0) {
                meta.setLore(List.of(lores));
            }
            try {
                var comp = meta.getCustomModelDataComponent();
                comp.setStrings(List.of(cmd));
                meta.setCustomModelDataComponent(comp);
            } catch (Throwable ignored) {
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    // 룰렛 상태 변경 메서드
    public void toggleColor() {
        selectedColor = "빨강".equals(selectedColor) ? "검정" : "빨강";
        updateWheelItems();
    }

    public void cycleBundle() {
        selectedBundle = switch (selectedBundle) {
            case "1~12" -> "13~24";
            case "13~24" -> "25~36";
            default -> "1~12";
        };
        updateWheelItems();
    }

    public void cycleNumber(boolean reverse) {
        if (reverse) {
            selectedNumber = (selectedNumber <= 1) ? 36 : (selectedNumber - 1);
        } else {
            selectedNumber = (selectedNumber >= 36) ? 1 : (selectedNumber + 1);
        }
        updateWheelItems();
    }

    public String getSelectedColor() {
        return selectedColor;
    }

    public String getSelectedBundle() {
        return selectedBundle;
    }

    public int getSelectedNumber() {
        return selectedNumber;
    }

    public int getCurrentWheelNumber() {
        return currentWheelNumber;
    }

    public void setCurrentWheelNumber(int currentWheelNumber) {
        this.currentWheelNumber = currentWheelNumber;
    }

    public boolean isSpinning() {
        return isSpinning;
    }

    public void setSpinning(boolean spinning) {
        this.isSpinning = spinning;
    }

    public BukkitTask getSpinningTask() {
        return spinningTask;
    }

    public void setSpinningTask(BukkitTask spinningTask) {
        this.spinningTask = spinningTask;
    }

    public HoldemGameSession getHoldemSession() {
        return holdemSession;
    }

    public boolean isSlotSpinning() {
        return isSlotSpinning;
    }

    public void setSlotSpinning(boolean slotSpinning) {
        this.isSlotSpinning = slotSpinning;
    }

    public BukkitTask getSlotSpinTask() {
        return slotSpinTask;
    }

    public void setSlotSpinTask(BukkitTask slotSpinTask) {
        this.slotSpinTask = slotSpinTask;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public CasinoType getType() {
        return type;
    }

    public Player getPlayer() {
        return player;
    }
}
