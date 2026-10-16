package Hez.Menu;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class MailboxMenu implements InventoryHolder {

    private static final Key FONT_DEFAULT = Key.key("minecraft", "default");
    private final Inventory inventory;

    public MailboxMenu() {
        // [\\uEBBB][\\uE300] 큰 상자 크기 타이틀 (WHITE, 그림자 제거)
        Component title = Component.text()
                .append(Component.text("\uEBBB\uE300")
                        .font(FONT_DEFAULT)
                        .color(NamedTextColor.WHITE)
                        .shadowColor(ShadowColor.none()))
                .build();

        // 큰 상자 크기 (54칸, 9 x 6)
        this.inventory = Bukkit.createInventory(this, 54, title);
    }

    public static void open(Player player) {
        MailboxMenu menu = new MailboxMenu();
        player.openInventory(menu.getInventory());
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }
}
