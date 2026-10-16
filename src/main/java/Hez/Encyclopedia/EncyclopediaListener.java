package Hez.Encyclopedia;

import Hez.MineFarm.Main;
import Hez.Player.PlayerData;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public class EncyclopediaListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof EncyclopediaHolder holder)) {
            return;
        }

        // 도감 GUI가 열려있는 동안에는 기본 인벤토리 조작(아이템 옮기기 등) 취소
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // 1. 플레이어 자신의 인벤토리(하단)에서 아이템을 클릭한 경우 -> 도감 등록 시도
        if (event.getClickedInventory() == event.getView().getBottomInventory()) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) {
                return;
            }

            // 도감 리스트에서 매칭되는 아이템 조회
            EncyclopediaItem match = DefaultEncyclopediaRegistry.findMatchingItem(clicked);
            if (match == null) {
                player.sendMessage(Component.text("§c도감에 등록할 수 없는 아이템입니다."));
                return;
            }

            PlayerData pData = Main.getInstance().getPlayerManager().getOrCreatePlayerData(player.getUniqueId());
            PlayerEncyclopedia enc = pData.encyclopedia();

            // 이미 등록되어 있는지 비트마스크 확인
            if (enc.isUnlocked(EncyclopediaType.DEFAULT, match.index())) {
                player.sendMessage(Component.text("§c이미 도감에 등록된 아이템입니다. (" + match.name() + ")"));
                return;
            }

            int required = match.requiredAmount();

            // 1. 클릭한 스택 단독으로 수량을 만족하는 경우
            if (clicked.getAmount() >= required) {
                clicked.subtract(required);
            } else {
                // 2. 여러 슬롯에 나뉘어 있을 경우 전체 인벤토리에서 동일 아이템 수량 합산 확인
                int totalOwned = 0;
                for (ItemStack invItem : player.getInventory().getContents()) {
                    if (invItem != null && !invItem.getType().isAir()) {
                        EncyclopediaItem itemMatch = DefaultEncyclopediaRegistry.findMatchingItem(invItem);
                        if (itemMatch != null && itemMatch.index() == match.index()) {
                            totalOwned += invItem.getAmount();
                        }
                    }
                }

                if (totalOwned < required) {
                    player.sendMessage(Component.text("§c" + match.name() + " 이(가) 부족합니다. (" + totalOwned + "/" + required + "개 보유)"));
                    return;
                }

                // 전체 인벤토리에서 차감 진행
                int toDeduct = required;
                ItemStack[] contents = player.getInventory().getContents();
                for (int i = 0; i < contents.length; i++) {
                    ItemStack invItem = contents[i];
                    if (invItem != null && !invItem.getType().isAir()) {
                        EncyclopediaItem itemMatch = DefaultEncyclopediaRegistry.findMatchingItem(invItem);
                        if (itemMatch != null && itemMatch.index() == match.index()) {
                            int take = Math.min(toDeduct, invItem.getAmount());
                            invItem.subtract(take);
                            toDeduct -= take;
                            if (toDeduct <= 0) break;
                        }
                    }
                }
            }

            // 비트마스크 등록 (1 << bit)
            enc.setUnlocked(EncyclopediaType.DEFAULT, match.index(), true);

            // 성공 피드백
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            player.sendMessage(Component.text("§a[도감] " + match.name() + " (" + required + "개) 이(가) 도감에 등록되었습니다! (" + enc.getUnlockedCount(EncyclopediaType.DEFAULT) + "/432)"));

            // 현재 열려있는 도감 GUI 슬롯 즉시 갱신 (customModelData 0 -> 1 로 보여짐)
            EncyclopediaMenu.renderPage(player, event.getView().getTopInventory(), holder.getPage());
            return;
        }

        // 2. 도감 상단 GUI(상자 인벤토리)를 클릭한 경우
        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            int slot = event.getSlot();

            if (slot == EncyclopediaMenu.SLOT_PREV) {
                int currentPage = holder.getPage();
                if (currentPage > 1) {
                    EncyclopediaMenu.openDefaultEncyclopedia(player, currentPage - 1);
                }
            } else if (slot == EncyclopediaMenu.SLOT_NEXT) {
                int currentPage = holder.getPage();
                int totalPages = (int) Math.ceil((double) DefaultEncyclopediaRegistry.getTotalItemCount() / EncyclopediaMenu.ITEMS_PER_PAGE);
                if (totalPages < 1) totalPages = 1;

                if (currentPage < totalPages) {
                    EncyclopediaMenu.openDefaultEncyclopedia(player, currentPage + 1);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof EncyclopediaHolder) {
            event.setCancelled(true);
        }
    }
}
