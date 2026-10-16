package Hez.Menu;

import Hez.CustomItem.CreateItem;
import Hez.House.HouseData;
import Hez.House.HouseManager;
import Hez.MineFarm.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * 머리 클릭 시 나타나는 1 x 5 (호퍼 크기) 관리/확인 메뉴
 * - 1번 자리 (슬롯 0): 커스텀 페이퍼 1006 (X 표시 / 취소)
 * - 3번 자리 (슬롯 2): 클릭한 사람 머리
 * - 5번 자리 (슬롯 4): 커스텀 페이퍼 1005 (확인 / 멤버 추가 또는 추방)
 */
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;

public class HouseMemberActionMenu implements InventoryHolder {

    private static final Key FONT_DEFAULT = Key.key("minecraft", "default");
    public static final NamespacedKey TARGET_UUID_KEY = new NamespacedKey(Main.getInstance(), "member_target_uuid");

    private final Inventory inventory;
    private final Player player;
    private final OfflinePlayer target;
    private final boolean isMember;

    public HouseMemberActionMenu(Player player, OfflinePlayer target) {
        this.player = player;
        this.target = target;

        HouseManager houseManager = Main.getInstance().getHouseManager();
        HouseData house = houseManager.getOrCreateHouseData(player.getUniqueId());
        this.isMember = house.isMember(target.getUniqueId());

        // [\uE201] color : white (그림자 제거)
        Component title = Component.text()
                .append(Component.text("\uEBBB\uE201")
                        .font(FONT_DEFAULT)
                        .color(NamedTextColor.WHITE)
                        .shadowColor(ShadowColor.none()))
                .build();

        // 9 x 2 (18칸) 상자 인벤토리 생성
        this.inventory = Bukkit.createInventory(this, 18, title);
        setupItems();
    }

    private void setupItems() {
        // [1행 가운데 5칸 중 1번째 / 슬롯 인덱스 2] : 커스텀아이템 1006 (X 표시 / 취소)
        ItemStack cancelItem = CreateItem.create(Material.PAPER, "1006", "§c취소 (뒤로가기)");
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.displayName(Component.text("§c[ 취소 ]").decoration(TextDecoration.ITALIC, false));
            cancelMeta.lore(List.of(
                    Component.text("§7클릭 시 이전 메뉴로 돌아갑니다.").decoration(TextDecoration.ITALIC, false)
            ));
            cancelMeta.getPersistentDataContainer().set(CustomMenu.ACTION_KEY, PersistentDataType.STRING, "member_action_cancel");
            cancelItem.setItemMeta(cancelMeta);
        }
        inventory.setItem(2, cancelItem);

        // [1행 가운데 5칸 중 3번째 / 슬롯 인덱스 4] : 클릭한 사람 머리
        ItemStack targetHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) targetHead.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(target);
            String name = (target.getName() != null) ? target.getName() : "알 수 없음";
            skullMeta.displayName(Component.text((isMember ? "§a" : "§f") + name).decoration(TextDecoration.ITALIC, false));
            skullMeta.lore(List.of(
                    Component.text(isMember ? "§a상태: 영지 멤버" : "§7상태: 외지인 (방문자)").decoration(TextDecoration.ITALIC, false),
                    Component.text(target.isOnline() ? "§a● 현재 온라인" : "§7○ 현재 오프라인").decoration(TextDecoration.ITALIC, false)
            ));
            targetHead.setItemMeta(skullMeta);
        }
        inventory.setItem(4, targetHead);

        // [1행 가운데 5칸 중 5번째 / 슬롯 인덱스 6] : 커스텀아이템 1005 (확인 버튼)
        String actionTitle = isMember ? "§c[ 멤버 추방 ]" : "§a[ 멤버 추가 ]";
        String actionLore = isMember ? "§7클릭 시 해당 플레이어를 영지 멤버에서 제거합니다." : "§7클릭 시 해당 플레이어를 영지 멤버로 추가합니다.";
        ItemStack confirmItem = CreateItem.create(Material.PAPER, "1005", actionTitle);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.displayName(Component.text(actionTitle).decoration(TextDecoration.ITALIC, false));
            confirmMeta.lore(List.of(
                    Component.text(actionLore).decoration(TextDecoration.ITALIC, false)
            ));
            confirmMeta.getPersistentDataContainer().set(CustomMenu.ACTION_KEY, PersistentDataType.STRING, "member_action_confirm");
            confirmItem.setItemMeta(confirmMeta);
        }
        inventory.setItem(6, confirmItem);
    }

    public OfflinePlayer getTarget() {
        return target;
    }

    public boolean isMember() {
        return isMember;
    }

    public static void open(Player player, OfflinePlayer target) {
        HouseMemberActionMenu menu = new HouseMemberActionMenu(player, target);
        player.openInventory(menu.getInventory());
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }
}
