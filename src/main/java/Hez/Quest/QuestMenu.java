package Hez.Quest;

import Hez.CustomItem.CreateItem;
import Hez.Menu.CustomMenu;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class QuestMenu implements InventoryHolder {

    private static final Key FONT_DEFAULT = Key.key("minecraft", "default");
    private final Inventory inventory;

    public QuestMenu(Player player) {
        // [\uE40A][\uE400] 큰 상자 크기 타이틀 (WHITE, 그림자 제거)
        Component title = Component.text()
                .append(Component.text("\uE40A\uE400")
                        .font(FONT_DEFAULT)
                        .color(NamedTextColor.WHITE)
                        .shadowColor(ShadowColor.none()))
                .build();

        // 큰 상자 크기 (54칸, 9 x 6)
        this.inventory = Bukkit.createInventory(this, 54, title);

        // 상단 상자 6개 행 (Row 0 ~ Row 5) 배치
        int[] slots = QuestLayoutManager.getInstance().getSlots();
        for (int row = 0; row < 6; row++) {
            int col = slots[row];
            int slot = row * 9 + col;
            this.inventory.setItem(slot, createQuestItem("1008"));
        }
    }

    /**
     * 퀘스트 메뉴 오픈
     * 1. 원래 인벤토리 안전 백업 및 비우기
     * 2. 상단 상자(0~5행) 및 하단 인벤토리(6~9행)에 지정 아이템 배치
     * 3. 인벤토리 오픈
     */
    public static void open(Player player) {
        // 1. 인벤토리(보관함 36칸) 백업 및 비우기
        QuestInventoryManager.getInstance().backupAndClear(player);

        // 2. 상단 상자 메뉴 생성 (0~5행 1008 아이템 배치 완료)
        QuestMenu menu = new QuestMenu(player);

        // 3. 하단 플레이어 인벤토리 배치 (6~9행)
        int[] slots = QuestLayoutManager.getInstance().getSlots();
        PlayerInventory playerInv = player.getInventory();

        // 행 7 (보관함 1행, 슬롯 9 ~ 17): 랜덤 위치에 1008 아이템
        playerInv.setItem(9 + slots[6], createQuestItem("1008"));

        // 행 8 (보관함 2행, 슬롯 18 ~ 26): 랜덤 위치에 1008 아이템
        playerInv.setItem(18 + slots[7], createQuestItem("1008"));

        // 행 9 (보관함 3행, 슬롯 27 ~ 35): 랜덤 위치에 1008 아이템
        playerInv.setItem(27 + slots[8], createQuestItem("1008"));

        // 행 10 (마지막 행 = 핫바 / 메인손 라인, 슬롯 0 ~ 8):
        // 1008은 안 두고, 4번(인덱스 3)에 1003, 6번(인덱스 5)에 1004 아이템 배치
        playerInv.setItem(3, createQuestItem("1003"));
        playerInv.setItem(5, createQuestItem("1004"));

        player.updateInventory();

        // 4. GUI 오픈
        org.bukkit.inventory.InventoryView view = player.openInventory(menu.getInventory());
        if (view == null) {
            // 인벤토리 열기 실패 시 즉시 복구
            QuestInventoryManager.getInstance().restore(player);
            return;
        }
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
    }

    /**
     * 커스텀 퀘스트 페이퍼 아이템 생성 (Material.PAPER, CustomModelData)
     */
    public static ItemStack createQuestItem(String cmd) {
        ItemStack item = CreateItem.create(Material.PAPER, cmd, "");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty().decoration(TextDecoration.ITALIC, false));
            meta.getPersistentDataContainer().set(CustomMenu.ACTION_KEY, PersistentDataType.STRING, "quest_item_" + cmd);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }
}
