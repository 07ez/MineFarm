package Hez.Menu;

import Hez.House.HouseData;
import Hez.House.HouseManager;
import Hez.Quest.QuestMenu;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MenuListener implements Listener {

    // 중복 호출 방지용 쿨다운 (Shift+F 및 마우스 연타/더블클릭 방지)
    private static final Map<UUID, Long> openCooldown = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> clickCooldown = new ConcurrentHashMap<>();

    /**
     * 웅크리기(Shift) + F키 (스왑 핸드) 감지 시 9x6 커스텀 GUI 메뉴 오픈
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();

        if (player.isSneaking()) {
            event.setCancelled(true);

            long now = System.currentTimeMillis();
            if (now - openCooldown.getOrDefault(player.getUniqueId(), 0L) < 250) {
                return;
            }
            openCooldown.put(player.getUniqueId(), now);

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

        // 마인크래프트의 더블클릭(아이템 모으기) 패킷 무시
        if (event.getClick() == ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
            return;
        }

        if (event.getInventory().getHolder() instanceof MailboxMenu) {
            // 우편함 메뉴창 안의 아이템 조작 금지
            event.setCancelled(true);
            return;
        }
        if (event.getInventory().getHolder() instanceof QuestMenu) {
            // 퀘스트 메뉴창 안의 아이템 조작 금지
            event.setCancelled(true);
            return;
        }
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
                    // 28번: 본인이 소유한 영지(집) 안에서만 TP 위치 설정 가능
                    HouseManager houseMgr = Hez.MineFarm.Main.getInstance().getHouseManager();
                    org.bukkit.Location currentLoc = player.getLocation();
                    HouseData locHouse = houseMgr.GetHouseData(currentLoc);

                    if (locHouse == null || !locHouse.isOwner(player.getUniqueId())) {
                        player.sendMessage("§c집 TP 위치는 본인이 소유한 영지(집) 안에서만 설정할 수 있습니다!");
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        break;
                    }

                    house.setSpawnLocation(currentLoc.clone());
                    houseMenu.updateTpItem();
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                    player.sendMessage(String.format("§a[집관리] 현재 위치(X: %d, Y: %d, Z: %d)가 집 TP 위치로 설정되었습니다!",
                            currentLoc.getBlockX(), currentLoc.getBlockY(), currentLoc.getBlockZ()));
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

        // 마우스 클릭 쿨다운 (0.15초 이내 중복 클릭 차단)
        long now = System.currentTimeMillis();
        if (now - clickCooldown.getOrDefault(player.getUniqueId(), 0L) < 150) {
            return;
        }
        clickCooldown.put(player.getUniqueId(), now);

        boolean isWarp = false;
        boolean isSubMenu = false;

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
                    isSubMenu = true;
                    Hez.Encyclopedia.EncyclopediaMenu.openDefaultEncyclopedia(player);
                    break;
                case "house_manage":
                    isSubMenu = true;
                    HouseManageMenu.open(player);
                    break;
                case "mailbox":
                    isSubMenu = true;
                    MailboxMenu.open(player);
                    break;
                case "quest":
                    isSubMenu = true;
                    QuestMenu.open(player);
                    break;
                case "pet":
                    break;
                case "house":
                    HouseManager hMgr = Hez.MineFarm.Main.getInstance().getHouseManager();
                    HouseData pHouse = hMgr.getHouseByOwner(player.getUniqueId());
                    org.bukkit.Location homeSpawn = (pHouse != null) ? pHouse.getSpawnLocation() : null;

                    if (homeSpawn == null || homeSpawn.getWorld() == null) {
                        player.sendMessage("§c설정된 집 위치가 없습니다! 먼저 집관리 메뉴에서 TP 위치를 설정해주세요.");
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        break;
                    }

                    isWarp = true;
                    player.closeInventory();
                    player.teleport(homeSpawn);
                    player.sendMessage("§a집으로 순간이동했습니다!");
                    player.playSound(homeSpawn, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    break;
                case "wiki":
                case "discord":
                case "bug_report":
                default:
                    break;
            }
        }

        // 워프(엔더맨 소리)와 서브메뉴(오픈 소리)를 제외한 일반 버튼 클릭 시에만 "띵" 소리 1회 재생!
        if (!isWarp && !isSubMenu) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.4f);
        }
    }
}
