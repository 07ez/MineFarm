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

import java.util.ArrayList;
import java.util.List;

public class QuestMenu implements InventoryHolder {

    private static final Key FONT_DEFAULT = Key.key("minecraft", "default");
    private final Inventory inventory;
    private int currentPage = 1;

    public QuestMenu() {
        // [\uE40A][\uE400] 큰 상자 크기 타이틀 (WHITE, 그림자 제거)
        Component title = Component.text()
                .append(Component.text("\uE40A\uE400")
                        .font(FONT_DEFAULT)
                        .color(NamedTextColor.WHITE)
                        .shadowColor(ShadowColor.none()))
                .build();

        // 큰 상자 크기 (54칸, 9 x 6)
        this.inventory = Bukkit.createInventory(this, 54, title);
    }

    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * 특정 페이지(1~3)를 상자 및 플레이어 인벤토리에 렌더링
     */
    public void renderPage(Player player, int page) {
        this.currentPage = Math.max(1, Math.min(QuestLayoutManager.MAX_PAGE, page));
        int[] slots = QuestLayoutManager.getInstance().getSlots(this.currentPage);

        // 1. 상단 상자 (0~5행, 54칸) 렌더링 (1~6행 퀘스트 노드)
        this.inventory.clear();
        for (int row = 0; row < 6; row++) {
            int col = slots[row];
            int slot = row * 9 + col;
            this.inventory.setItem(slot, createQuestNodeItem(player, this.currentPage, row + 1));
        }

        // 2. 하단 플레이어 인벤토리 (6~9행) 렌더링
        PlayerInventory playerInv = player.getInventory();

        // 보관함 3줄(인벤토리 슬롯 9 ~ 35) 비우기
        for (int s = 9; s < 36; s++) {
            playerInv.setItem(s, null);
        }

        // 행 7 (보관함 1행, 슬롯 9 ~ 17): 7행 퀘스트 노드
        playerInv.setItem(9 + slots[6], createQuestNodeItem(player, this.currentPage, 7));

        // 행 8 (보관함 2행, 슬롯 18 ~ 26): 8행 퀘스트 노드
        playerInv.setItem(18 + slots[7], createQuestNodeItem(player, this.currentPage, 8));

        // 행 9 (보관함 3행, 슬롯 27 ~ 35): 9행 퀘스트 노드
        playerInv.setItem(27 + slots[8], createQuestNodeItem(player, this.currentPage, 9));

        // 행 10 (핫바 / 메인손 라인, 슬롯 0 ~ 8):
        // 핫바 슬롯 초기화
        for (int s = 0; s < 9; s++) {
            playerInv.setItem(s, null);
        }

        // 버튼 숨기기 규칙:
        // 4번 자리 (슬롯 3): 1페이지면 숨김(null), 2페이지 이상일 때만 뒤화살표(1003) 표시
        if (this.currentPage > 1) {
            playerInv.setItem(3, createQuestItem("1003"));
        }

        // 6번 자리 (슬롯 5): 마지막 페이지(3페이지)면 숨김(null), 그 외에는 다음화살표(1004) 표시
        if (this.currentPage < QuestLayoutManager.MAX_PAGE) {
            playerInv.setItem(5, createQuestItem("1004"));
        }

        player.updateInventory();
    }

    /**
     * 페이지 변경 (클릭 시 호출)
     */
    public void changePage(Player player, int newPage) {
        if (newPage < 1 || newPage > QuestLayoutManager.MAX_PAGE) return;
        renderPage(player, newPage);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }

    /**
     * 퀘스트 메뉴 오픈
     */
    public static void open(Player player) {
        // 1. 인벤토리(보관함 36칸) 백업 및 비우기
        QuestInventoryManager.getInstance().backupAndClear(player);

        // 2. 퀘스트 메뉴 인스턴스 생성 및 1페이지 렌더링
        QuestMenu menu = new QuestMenu();
        menu.renderPage(player, 1);

        // 3. GUI 오픈
        org.bukkit.inventory.InventoryView view = player.openInventory(menu.getInventory());
        if (view == null) {
            // 인벤토리 열기 실패 시 즉시 복구
            QuestInventoryManager.getInstance().restore(player);
            return;
        }
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
    }

    /**
     * 퀘스트 노드 아이템 생성 (제목: 이름, 내용: lore, 마지막 줄: 상태)
     * - 클리어 시: 1007 (별)
     * - 미완료/진행중 시: 1008 (어두운별)
     */
    public static ItemStack createQuestNodeItem(Player player, int page, int row) {
        QuestData quest = QuestManager.getInstance().getQuest(page, row);
        if (quest == null) {
            return createQuestItem("1008");
        }

        QuestStatus status = QuestManager.getInstance().getQuestStatus(player.getUniqueId(), quest.getId());
        String cmd = (status == QuestStatus.COMPLETED) ? "1007" : "1008";

        ItemStack item = CreateItem.create(Material.PAPER, cmd, "");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // 제목을 아이템 이름으로 설정
            meta.displayName(Component.text("§e" + quest.getId() + ". " + quest.getTitle())
                    .decoration(TextDecoration.ITALIC, false));

            // 내용을 Lore로 설정 + 마지막 줄에 상태 추가
            List<Component> lore = new ArrayList<>();
            for (String obj : quest.getObjectives()) {
                lore.add(Component.text("§7- " + obj).decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.empty());
            lore.add(Component.text(status.getFormattedText()).decoration(TextDecoration.ITALIC, false));

            meta.lore(lore);
            meta.getPersistentDataContainer().set(CustomMenu.ACTION_KEY, PersistentDataType.STRING, "quest_node_" + quest.getId());
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 커스텀 퀘스트 페이퍼 아이템 생성 (버튼용)
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
