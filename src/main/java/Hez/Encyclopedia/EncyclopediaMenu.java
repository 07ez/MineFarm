package Hez.Encyclopedia;

import Hez.MineFarm.Main;
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
     * - 어둡게 보이지 않도록 전체 및 하위 모두 color: WHITE 설정
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
     * - 6번째 줄 양끝(슬롯 45, 53)에 임시 유리판("이전으로", "다음으로") 배치
     */
    public static void openDefaultEncyclopedia(Player player, int page) {
        if (page < 1) page = 1;

        EncyclopediaHolder holder = new EncyclopediaHolder(EncyclopediaType.DEFAULT, page);
        Inventory inv = Bukkit.createInventory(holder, INVENTORY_SIZE, DEFAULT_TITLE);
        holder.setInventory(inv);

        // 마지막 6번째 줄 양끝: 이전으로 (슬롯 45), 다음으로 (슬롯 53) 임시 유리판 배치
        inv.setItem(SLOT_PREV, createButton(Material.GLASS_PANE, "이전으로", ACTION_PREV));
        inv.setItem(SLOT_NEXT, createButton(Material.GLASS_PANE, "다음으로", ACTION_NEXT));

        player.openInventory(inv);
    }

    /**
     * 임시 버튼 아이템 생성 헬퍼
     */
    private static ItemStack createButton(Material material, String name, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
            meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, action);
            item.setItemMeta(meta);
        }
        return item;
    }
}
