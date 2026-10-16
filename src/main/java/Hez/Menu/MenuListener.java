package Hez.Menu;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class MenuListener implements Listener {

    /**
     * 웅크리기(Shift) + F키 (스왑 핸드) 감지 시 9x6 커스텀 GUI 메뉴 오픈
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();

        if (player.isSneaking()) {
            event.setCancelled(true);
            CustomMenu.open(player);
        }
    }

    /**
     * 메뉴 인벤토리 클릭 이벤트
     * - 메뉴 내 아이템 꺼내기/이동 방지
     * - 워프 항목은 텔레포트(엔더맨 소리), 그 외 항목은 "띵" 소리 재생
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMenuClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof HouseManageMenu) {
            // 집관리 메뉴창 안의 아이템 조작 금지
            event.setCancelled(true);
            return;
        }

        if (!(event.getInventory().getHolder() instanceof CustomMenu)) {
            return;
        }

        // 메뉴창 안의 아이템 조작 금지
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        boolean isWarp = false;
        String action = clicked.getItemMeta().getPersistentDataContainer().get(CustomMenu.ACTION_KEY, PersistentDataType.STRING);
        if (action != null) {
            switch (action) {
                case "town":
                case "wild":
                case "museum":
                case "bank":
                case "crops":
                case "fish":
                case "ore":
                case "tree":
                    isWarp = true;
                    WarpManager.teleport(player, action);
                    break;
                case "encyclopedia":
                    Hez.Encyclopedia.EncyclopediaMenu.openDefaultEncyclopedia(player);
                    break;
                case "house_manage":
                    HouseManageMenu.open(player);
                    break;
                case "mailbox":
                case "quest":
                case "pet":
                case "house":
                case "wiki":
                case "discord":
                case "bug_report":
                default:
                    break;
            }
        }

        // 텔레포트(워프)를 제외한 메뉴 버튼을 클릭했을 때 "띵" 효과음 재생!
        if (!isWarp) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.4f);
        }
    }
}
