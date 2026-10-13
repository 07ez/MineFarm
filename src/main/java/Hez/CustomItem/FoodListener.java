package Hez.CustomItem;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

import java.util.List;

/**
 * 커스텀 요리 섭취 시 포만감, 포화도, 고유 버프 효과를 적용하는 리스너
 */
public class FoodListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item.getType() != Material.APPLE || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        CustomModelDataComponent cmd = meta.getCustomModelDataComponent();
        List<String> strings = cmd.getStrings();
        if (strings == null || strings.isEmpty()) return;

        String modelData = strings.get(0);
        Player player = event.getPlayer();

        // 1. 밀가루(1001)는 요리 재료이므로 먹는 것 방지
        if ("1001".equals(modelData)) {
            event.setCancelled(true);
            player.sendMessage("§c[요리] 밀가루는 생으로 먹을 수 없는 요리 재료입니다!");
            return;
        }

        // 2. 커스텀 요리 스펙 조회
        FoodManager.FoodSpec spec = FoodManager.getFoodSpec(modelData);
        if (spec == null) return;

        // 3. 기본 사과의 배고픔(+4)/포화도(+2.4) 대신 커스텀 요리의 정확한 수치 반영 보장
        // 이벤트 취소 없이 아이템이 정상 소모되도록 하되, 1틱 뒤 또는 즉시 정확한 수치를 계산해 적용
        int beforeFood = player.getFoodLevel();
        float beforeSat = player.getSaturation();

        // 바닐라 사과 섭취 처리를 취소하고 직접 아이템 1개 차감 + 정확한 수치/버프 적용
        event.setCancelled(true);

        // 손에 든 아이템 1개 감소
        ItemStack handItem = player.getInventory().getItem(event.getHand());
        if (handItem != null && handItem.getAmount() > 1) {
            handItem.setAmount(handItem.getAmount() - 1);
        } else {
            player.getInventory().setItem(event.getHand(), null);
        }

        // 배고픔 적용 (최대 20)
        int newFood = Math.min(20, beforeFood + spec.nutrition());
        player.setFoodLevel(newFood);

        // 포화도(노란색 추가 게이지) 적용 (마인크래프트 규칙상 현재 배고픔 수치를 초과할 수 없음, 최대 20.0)
        float newSat = Math.min((float) newFood, beforeSat + spec.saturation());
        player.setSaturation(newSat);

        // 4. 버프 효과 부여
        for (FoodManager.BuffSpec buff : spec.buffs()) {
            player.addPotionEffect(buff.toPotionEffect());
        }

        // 트림 사운드 및 메시지
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0f, 1.0f);
    }
}
