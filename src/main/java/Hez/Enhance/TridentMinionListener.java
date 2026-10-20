package Hez.Enhance;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class TridentMinionListener implements Listener {

    /**
     * 플레이어가 삼지창을 차징 후 던졌을 때 미니언 사출 발동 및 본체 보존
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTridentLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Trident)) return;
        if (!(event.getEntity().getShooter() instanceof Player player)) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (EnhanceEquipmentType.fromItem(held) != EnhanceEquipmentType.TRIDENT) return;

        int level = EnhanceManager.getEnhanceLevel(held);
        if (level <= 0) return;

        // 바닐라 삼지창 투척 취소 (손에 든 삼지창 본체 보존!)
        event.setCancelled(true);

        // 뒤에 소환되어 있던 미니언 삼지창들을 에임 방향으로 발사!
        TridentMinionManager.fireMinions(player, held);
    }

    /**
     * 손에 든 슬롯을 다른 아이템으로 바꿀 때 기존 미니언 제거
     */
    @EventHandler
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());

        if (EnhanceEquipmentType.fromItem(newItem) != EnhanceEquipmentType.TRIDENT) {
            TridentMinionManager.clearMinions(player);
        }
    }

    /**
     * 아이템을 바닥에 버릴 때 미니언 제거
     */
    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        ItemStack dropped = event.getItemDrop().getItemStack();
        if (dropped.getType() == Material.TRIDENT) {
            TridentMinionManager.clearMinions(event.getPlayer());
        }
    }

    /**
     * 플레이어가 서버에서 나갈 때 미니언 정리
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        TridentMinionManager.clearMinions(event.getPlayer());
    }
}
