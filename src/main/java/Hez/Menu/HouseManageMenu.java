package Hez.Menu;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class HouseManageMenu implements InventoryHolder {

    private static final Key FONT_DEFAULT = Key.key("minecraft", "default");
    private final Inventory inventory;

    public HouseManageMenu() {
        // [\uEBBB][\uE200] color : white (그림자 제거)
        Component title = Component.text()
                .append(Component.text("\uEBBB\uE200")
                        .font(FONT_DEFAULT)
                        .color(NamedTextColor.WHITE)
                        .shadowColor(ShadowColor.none()))
                .build();

        // 9 x 6 (54칸) 상자 생성
        this.inventory = Bukkit.createInventory(this, 54, title);
    }

    public static void open(Player player) {
        HouseManageMenu menu = new HouseManageMenu();
        player.openInventory(menu.getInventory());
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }
}
