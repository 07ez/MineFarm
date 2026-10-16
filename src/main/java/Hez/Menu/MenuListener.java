package Hez.Menu;

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
     * - 태그(PDC) 또는 제목(DisplayName)으로 버튼 인식 예시
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMenuClick(InventoryClickEvent event) {
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

        // 1. [가장 추천] PDC 태그로 인식하는 방법:
        String action = clicked.getItemMeta().getPersistentDataContainer().get(CustomMenu.ACTION_KEY, PersistentDataType.STRING);
        if (action != null) {
            // 나중에 기능 연결 시 사용할 수 있도록 구조화
            switch (action) {
                case "mailbox":
                case "quest":
                    break;
                case "encyclopedia":
                    Hez.Encyclopedia.EncyclopediaMenu.openDefaultEncyclopedia(player);
                    break;
                case "pet":
                case "house":
                    break;
                case "town":
                case "wild":
                case "museum":
                case "bank":
                case "crops":
                case "fish":
                case "ore":
                case "tree":
                    WarpManager.teleport(player, action);
                    break;
                case "wiki":
                case "discord":
                case "bug_report":
                    break;
            }
        }

        // 2. [참고] 제목(DisplayName)으로 인식하는 방법:
        // net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());
    }
}
