package Hez.Encyclopedia;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class EncyclopediaListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof EncyclopediaHolder holder)) {
            return;
        }

        // 도감 GUI 내 모든 아이템 이동/클릭 취소
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int rawSlot = event.getRawSlot();

        // 6번째 줄 양끝 페이지 이동 버튼 클릭 처리
        if (rawSlot == EncyclopediaMenu.SLOT_PREV) {
            int currentPage = holder.getPage();
            if (currentPage > 1) {
                EncyclopediaMenu.openDefaultEncyclopedia(player, currentPage - 1);
            }
        } else if (rawSlot == EncyclopediaMenu.SLOT_NEXT) {
            int currentPage = holder.getPage();
            EncyclopediaMenu.openDefaultEncyclopedia(player, currentPage + 1);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof EncyclopediaHolder) {
            event.setCancelled(true);
        }
    }
}
