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
        if (!(event.getEntity() instanceof Trident trident)) return;

        // 미니언 사출 삼지창은 발사를 허용하여 실제로 시원하게 날아가도록 함!
        if (trident.getScoreboardTags().contains("minion_trident")) {
            return;
        }

        if (!(trident.getShooter() instanceof Player player)) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (EnhanceEquipmentType.fromItem(held) != EnhanceEquipmentType.TRIDENT) return;

        int level = EnhanceManager.getEnhanceLevel(held);
        if (level <= 0) return;

        // 바닐라 삼지창 투척 취소 (손에 든 삼지창 본체 보존!)
        event.setCancelled(true);

        // 손에 든 삼지창 본체 인벤토리 보존
        ItemStack tridentClone = held.clone();
        org.bukkit.Bukkit.getScheduler().runTask(Hez.MineFarm.Main.getInstance(), () -> {
            if (player.isOnline()) {
                ItemStack cur = player.getInventory().getItemInMainHand();
                if (cur.getType() == Material.AIR || EnhanceEquipmentType.fromItem(cur) != EnhanceEquipmentType.TRIDENT) {
                    player.getInventory().setItemInMainHand(tridentClone);
                }
            }
        });

        // 뒤에서 회전/대기하던 미니언 삼지창들을 에임 방향으로 발사!
        TridentMinionManager.fireMinions(player, held);
    }

    /**
     * 사출된 미니언 삼지창이 적이나 블록에 적중했을 때 타격 연출 (충성 복귀가 작동하도록 remove()하지 않음)
     */
    @EventHandler
    public void onTridentHit(org.bukkit.event.entity.ProjectileHitEvent event) {
        if (event.getEntity() instanceof Trident trident && trident.getScoreboardTags().contains("minion_trident")) {
            org.bukkit.Location loc = trident.getLocation();
            loc.getWorld().spawnParticle(org.bukkit.Particle.CRIT, loc, 10, 0.2, 0.2, 0.2, 0.1);
            loc.getWorld().spawnParticle(org.bukkit.Particle.ELECTRIC_SPARK, loc, 6, 0.2, 0.2, 0.2, 0.05);
            loc.getWorld().playSound(loc, org.bukkit.Sound.ITEM_TRIDENT_HIT, 1.0f, 1.2f);
            // 충성(Loyalty) 효과에 의해 플레이어에게 빙글빙글 날아오도록 엔티티를 유지합니다.
        }
    }

    /**
     * 충성 효과로 복귀한 미니언 삼지창이 플레이어에게 닿을 때 인벤토리 픽업 방지 및 안전 소멸
     */
    @EventHandler
    public void onPlayerPickupArrow(org.bukkit.event.player.PlayerPickupArrowEvent event) {
        if (event.getArrow() instanceof Trident trident && trident.getScoreboardTags().contains("minion_trident")) {
            event.setCancelled(true);
            trident.remove();
        }
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
