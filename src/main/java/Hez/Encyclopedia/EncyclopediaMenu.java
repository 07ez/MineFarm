package Hez.Encyclopedia;

import Hez.MineFarm.Main;
import Hez.Player.PlayerData;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class EncyclopediaMenu {

    // 9x6 형태 (큰 상자: 54칸)
    public static final int INVENTORY_SIZE = 54;
    // 한 페이지당 도감 아이템 배치 슬롯 수 (0~44: 총 45칸)
    public static final int ITEMS_PER_PAGE = 45;

    // 6번째 줄 양끝 슬롯 인덱스 (0-based)
    public static final int SLOT_PREV = 45;
    public static final int SLOT_NEXT = 53;

    public static final NamespacedKey ACTION_KEY = new NamespacedKey(Main.getInstance(), "encyclopedia_action");
    public static final String ACTION_PREV = "prev";
    public static final String ACTION_NEXT = "next";

    private static final Key FONT_DEFAULT = Key.key("minecraft", "default");

    /**
     * 기본 도감 GUI 타이틀
     * - [\uEBBB][\uE100] : 음수 스페이서(\uEBBB) + GUI 배경 텍스처 문자(\uE100)
     * - 어둡게 보이지 않도록 color: WHITE 설정
     */
    public static final Component DEFAULT_TITLE = Component.text()
            .append(Component.text("\uEBBB\uE100")
                    .font(FONT_DEFAULT)
                    .color(NamedTextColor.WHITE)
                    .shadowColor(ShadowColor.none()))
            .color(NamedTextColor.WHITE)
            .build();

    /**
     * 1페이지 기본 도감 GUI 오픈
     */
    public static void openDefaultEncyclopedia(Player player) {
        openDefaultEncyclopedia(player, 1);
    }

    /**
     * 특정 페이지의 기본 도감 GUI 오픈
     */
    public static void openDefaultEncyclopedia(Player player, int page) {
        if (page < 1) page = 1;

        EncyclopediaHolder holder = new EncyclopediaHolder(EncyclopediaType.DEFAULT, page);
        Inventory inv = Bukkit.createInventory(holder, INVENTORY_SIZE, DEFAULT_TITLE);
        holder.setInventory(inv);

        // 아이템 렌더링
        renderPage(player, inv, page);

        player.openInventory(inv);
    }

    /**
     * 도감 인벤토리 내용 렌더링 / 갱신
     */
    public static void renderPage(Player player, Inventory inv, int page) {
        PlayerData pData = Main.getInstance().getPlayerManager().getOrCreatePlayerData(player.getUniqueId());
        PlayerEncyclopedia enc = pData.encyclopedia();

        int startIndex = (page - 1) * ITEMS_PER_PAGE + 1;
        int totalItems = DefaultEncyclopediaRegistry.getTotalItemCount();

        // 1~5번째 줄 (슬롯 0 ~ 44): 도감 아이템 배치
        for (int slot = 0; slot < ITEMS_PER_PAGE; slot++) {
            int itemIndex = startIndex + slot;
            if (itemIndex <= totalItems) {
                EncyclopediaItem info = DefaultEncyclopediaRegistry.getItem(itemIndex);
                if (info != null) {
                    boolean unlocked = enc.isUnlocked(EncyclopediaType.DEFAULT, itemIndex);
                    ItemStack item = createEncyclopediaSlotItem(info, unlocked);
                    inv.setItem(slot, item);
                } else {
                    inv.setItem(slot, null);
                }
            } else {
                inv.setItem(slot, null);
            }
        }

        // 6번째 줄: 가운데 비우기
        for (int slot = 46; slot <= 52; slot++) {
            inv.setItem(slot, null);
        }

        // 마지막 6번째 줄 양끝: 이전으로 (슬롯 45, 1003), 다음으로 (슬롯 53, 1004) 페이퍼 배치
        inv.setItem(SLOT_PREV, createButton(Material.PAPER, "1003", "이전으로", ACTION_PREV));
        inv.setItem(SLOT_NEXT, createButton(Material.PAPER, "1004", "다음으로", ACTION_NEXT));
    }

    /**
     * 도감 슬롯용 아이템 생성
     * - 미등록 시: Material.PAPER, customModelData = "1001" (CreateItem 방식)
     * - 등록 시: 실제 도감 아이템 Material 사용
     * - 이름은 마인팜도감리스트 이름 그대로
     * - lore는 보류
     */
    private static ItemStack createEncyclopediaSlotItem(EncyclopediaItem info, boolean unlocked) {
        if (!unlocked) {
            ItemStack item = Hez.CustomItem.CreateItem.create(Material.PAPER, "1001", info.name());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(info.name(), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
                item.setItemMeta(meta);
            }
            return item;
        }

        // 등록된 아이템: 실제 Material 사용
        ItemStack item = new ItemStack(info.material());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(info.name(), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 버튼 아이템 생성 헬퍼
     * - Material.PAPER, CustomModelDataComponent 적용
     */
    private static ItemStack createButton(Material material, String customModelData, String name, String action) {
        ItemStack item = Hez.CustomItem.CreateItem.create(material, customModelData, name);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
            meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, action);
            item.setItemMeta(meta);
        }
        return item;
    }
}
