package Hez.Menu;

import Hez.CustomItem.CreateItem;
import Hez.House.HouseData;
import Hez.House.HouseManager;
import Hez.MineFarm.Main;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HouseManageMenu implements InventoryHolder {

    private static final Key FONT_DEFAULT = Key.key("minecraft", "default");
    private final Inventory inventory;
    private final Player player;
    private final HouseData house;

    public HouseManageMenu(Player player) {
        this.player = player;
        HouseManager houseManager = Main.getInstance().getHouseManager();
        this.house = houseManager.getOrCreateHouseData(player.getUniqueId());

        // [\uEBBB][\uE200] color : white (그림자 제거)
        Component title = Component.text()
                .append(Component.text("\uEBBB\uE200")
                        .font(FONT_DEFAULT)
                        .color(NamedTextColor.WHITE)
                        .shadowColor(ShadowColor.none()))
                .build();

        // 9 x 6 (54칸) 상자 생성
        this.inventory = Bukkit.createInventory(this, 54, title);
        setupItems();
    }

    /**
     * UI 아이템 배치
     * - 슬롯 11: 내 머리
     * - 슬롯 5, 6, 7, 8, 9, 14, 15, 16, 17, 18: 멤버 머리 (10칸)
     * - 슬롯 28: TP 설정
     * - 슬롯 29: 문허용
     * - 슬롯 30: 상자허용
     * - 슬롯 32, 33, 34, 35, 36, 41, 42, 43, 44, 45: 외지인 머리 (10칸)
     */
    public void setupItems() {
        inventory.clear();

        // 1. [슬롯 11 (인덱스 10)] 내 머리
        ItemStack myHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta myMeta = (SkullMeta) myHead.getItemMeta();
        if (myMeta != null) {
            myMeta.setOwningPlayer(player);
            myMeta.displayName(Component.text("§e" + player.getName()).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7[영지 소유자]").decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("§f보유 청크: §a" + house.getChunks().size() + "개").decoration(TextDecoration.ITALIC, false));
            myMeta.lore(lore);
            myHead.setItemMeta(myMeta);
        }
        inventory.setItem(10, myHead);

        // 2. [슬롯 5, 6, 7, 8, 9, 14, 15, 16, 17, 18] 멤버 머리 (10칸)
        int[] memberSlots = {4, 5, 6, 7, 8, 13, 14, 15, 16, 17};
        int memberIdx = 0;
        for (UUID memberUuid : house.getMembers()) {
            if (memberIdx >= memberSlots.length) break;
            OfflinePlayer op = Bukkit.getOfflinePlayer(memberUuid);

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(op);
                String name = (op.getName() != null) ? op.getName() : "알 수 없음";
                meta.displayName(Component.text("§a" + name).decoration(TextDecoration.ITALIC, false));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("§7영지 멤버").decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text(op.isOnline() ? "§a● 온라인" : "§7○ 오프라인").decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("§e▶ 클릭하여 관리").decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
                meta.getPersistentDataContainer().set(HouseMemberActionMenu.TARGET_UUID_KEY, PersistentDataType.STRING, op.getUniqueId().toString());
                skull.setItemMeta(meta);
            }
            inventory.setItem(memberSlots[memberIdx], skull);
            memberIdx++;
        }

        // 3. [슬롯 28 (인덱스 27)] TP 설정
        updateTpItem();

        // 4. [슬롯 29 (인덱스 28)] 문허용 설정
        updateDoorItem();

        // 5. [슬롯 30 (인덱스 29)] 상자허용 설정
        updateContainerItem();

        // 6. [슬롯 32, 33, 34, 35, 36, 41, 42, 43, 44, 45] 외지인 머리 (10칸)
        int[] strangerSlots = {31, 32, 33, 34, 35, 40, 41, 42, 43, 44};
        int strangerIdx = 0;
        for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
            if (strangerIdx >= strangerSlots.length) break;
            if (op.getUniqueId().equals(player.getUniqueId())) continue;
            if (house.isMember(op.getUniqueId())) continue;
            if (op.getName() == null) continue;

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(op);
                meta.displayName(Component.text("§f" + op.getName()).decoration(TextDecoration.ITALIC, false));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("§7외지인 (방문자)").decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text(op.isOnline() ? "§a● 온라인" : "§7○ 오프라인").decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("§e▶ 클릭하여 관리").decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
                meta.getPersistentDataContainer().set(HouseMemberActionMenu.TARGET_UUID_KEY, PersistentDataType.STRING, op.getUniqueId().toString());
                skull.setItemMeta(meta);
            }
            inventory.setItem(strangerSlots[strangerIdx], skull);
            strangerIdx++;
        }
    }

    public void updateTpItem() {
        ItemStack tpItem = CreateItem.create(Material.PAPER, "1002", "§fTP 설정");
        ItemMeta meta = tpItem.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§fTP 설정").decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            Location tpLoc = house.getSpawnLocation();
            if (tpLoc != null && tpLoc.getWorld() != null) {
                lore.add(Component.text(String.format("§7현재 TP 위치: §fX: %d, Y: %d, Z: %d",
                        tpLoc.getBlockX(), tpLoc.getBlockY(), tpLoc.getBlockZ())).decoration(TextDecoration.ITALIC, false));
            } else {
                lore.add(Component.text("§7현재 TP 위치: §c미설정").decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.text("§e▶ 클릭 시 현재 위치로 설정").decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            meta.getPersistentDataContainer().set(CustomMenu.ACTION_KEY, PersistentDataType.STRING, "house_set_tp");
            tpItem.setItemMeta(meta);
        }
        inventory.setItem(27, tpItem);
    }

    public void updateDoorItem() {
        ItemStack doorItem = CreateItem.create(Material.PAPER, "1002", "§f문허용");
        ItemMeta meta = doorItem.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§f문허용").decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7현재 상태: " + house.getDoorAccessMode().getFormatted()).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("§e▶ 클릭하여 권한 변경").decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            meta.getPersistentDataContainer().set(CustomMenu.ACTION_KEY, PersistentDataType.STRING, "house_toggle_door");
            doorItem.setItemMeta(meta);
        }
        inventory.setItem(28, doorItem);
    }

    public void updateContainerItem() {
        ItemStack containerItem = CreateItem.create(Material.PAPER, "1002", "§f상자허용");
        ItemMeta meta = containerItem.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§f상자허용").decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7현재 상태: " + house.getContainerAccessMode().getFormatted()).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("§e▶ 클릭하여 권한 변경").decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            meta.getPersistentDataContainer().set(CustomMenu.ACTION_KEY, PersistentDataType.STRING, "house_toggle_container");
            containerItem.setItemMeta(meta);
        }
        inventory.setItem(29, containerItem);
    }

    public HouseData getHouse() {
        return house;
    }

    public Player getPlayer() {
        return player;
    }

    public static void open(Player player) {
        HouseManageMenu menu = new HouseManageMenu(player);
        player.openInventory(menu.getInventory());
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }
}
