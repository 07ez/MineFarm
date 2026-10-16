package Hez.Menu;

import Hez.House.HouseData;
import Hez.House.HouseManager;
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
        if (event.getInventory().getHolder() instanceof HouseMemberActionMenu actionMenu) {
            // 1x5 멤버 관리 확인창 조작 금지
            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;

            Player player = (Player) event.getWhoClicked();
            String action = clicked.getItemMeta().getPersistentDataContainer().get(CustomMenu.ACTION_KEY, PersistentDataType.STRING);
            if (action == null) return;

            if (action.equals("member_action_cancel")) {
                // 1번 자리 (1006): 취소 / 뒤로가기
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                HouseManageMenu.open(player);
            } else if (action.equals("member_action_confirm")) {
                // 5번 자리 (1005): 확인 / 멤버 추가 또는 추방
                HouseManager houseManager = Hez.MineFarm.Main.getInstance().getHouseManager();
                HouseData house = houseManager.getOrCreateHouseData(player.getUniqueId());
                org.bukkit.OfflinePlayer target = actionMenu.getTarget();
                String targetName = (target.getName() != null) ? target.getName() : "플레이어";

                if (actionMenu.isMember()) {
                    // 이미 멤버인 경우 -> 멤버 제외
                    house.removeMember(target.getUniqueId());
                    player.sendMessage("§e[집관리] " + targetName + "님을 영지 멤버에서 제외하였습니다.");
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.8f);
                } else {
                    // 외지인인 경우 -> 멤버 추가
                    house.addMember(target.getUniqueId());
                    player.sendMessage("§a[집관리] " + targetName + "님을 영지 멤버로 추가하였습니다!");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                }
                HouseManageMenu.open(player);
            }
            return;
        }

        if (event.getInventory().getHolder() instanceof HouseManageMenu houseMenu) {
            // 집관리 메뉴창 안의 아이템 조작 금지
            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) {
                return;
            }

            Player player = (Player) event.getWhoClicked();

            // 머리 클릭 감지 (멤버 머리 또는 외지인 머리)
            String targetUuidStr = clicked.getItemMeta().getPersistentDataContainer().get(HouseMemberActionMenu.TARGET_UUID_KEY, PersistentDataType.STRING);
            if (targetUuidStr != null) {
                try {
                    java.util.UUID targetUuid = java.util.UUID.fromString(targetUuidStr);
                    org.bukkit.OfflinePlayer targetOp = org.bukkit.Bukkit.getOfflinePlayer(targetUuid);
                    player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    HouseMemberActionMenu.open(player, targetOp);
                } catch (IllegalArgumentException ignored) {
                }
                return;
            }

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
