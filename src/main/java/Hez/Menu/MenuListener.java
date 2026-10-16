package Hez.Menu;

import Hez.House.HouseData;
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
        if (event.getInventory().getHolder() instanceof HouseManageMenu houseMenu) {
            // 집관리 메뉴창 안의 아이템 조작 금지
            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            String action = clicked.getItemMeta().getPersistentDataContainer().get(CustomMenu.ACTION_KEY, PersistentDataType.STRING);
            if (action == null) return;

            HouseData house = houseMenu.getHouse();

            switch (action) {
                case "house_set_tp":
                    // 28번: 현재 서 있는 위치를 집 TP 위치로 저장
                    house.setSpawnLocation(player.getLocation().clone());
                    houseMenu.updateTpItem();
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                    player.sendMessage(String.format("§a[집관리] 현재 위치(X: %d, Y: %d, Z: %d)가 집 TP 위치로 설정되었습니다!",
                            player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ()));
                    break;
                case "house_toggle_door":
                    // 29번: 아무도 허용x > 멤버만 가능 > 모두다 가능 > 아무도 허용x
                    house.setDoorAccessMode(house.getDoorAccessMode().next());
                    houseMenu.updateDoorItem();
                    player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    player.sendMessage("§a[집관리] 문 권한이 " + house.getDoorAccessMode().getFormatted() + "§a(으)로 변경되었습니다.");
                    break;
                case "house_toggle_container":
                    // 30번: 아무도 허용x > 멤버만 가능 > 모두다 가능 > 아무도 허용x
                    house.setContainerAccessMode(house.getContainerAccessMode().next());
                    houseMenu.updateContainerItem();
                    player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    player.sendMessage("§a[집관리] 상자 권한이 " + house.getContainerAccessMode().getFormatted() + "§a(으)로 변경되었습니다.");
                    break;
            }
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
