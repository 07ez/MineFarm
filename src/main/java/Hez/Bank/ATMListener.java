package Hez.Bank;

import Hez.MineFarm.Main;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ATMListener implements Listener {

    private final Main plugin;
    private final Map<UUID, Long> clickCooldown = new ConcurrentHashMap<>();

    public ATMListener(Main plugin) {
        this.plugin = plugin;
    }

    private boolean isATM(Entity entity) {
        if (entity == null) return false;
        return entity.getScoreboardTags().contains("atm");
    }

    private boolean checkCooldown(UUID uuid) {
        long now = System.currentTimeMillis();
        Long last = clickCooldown.get(uuid);
        if (last != null && now - last < 300L) {
            return false;
        }
        clickCooldown.put(uuid, now);
        return true;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!isATM(event.getRightClicked())) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!checkCooldown(player.getUniqueId())) return;

        ATMManager.openATMMenu(player);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!isATM(event.getRightClicked())) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!checkCooldown(player.getUniqueId())) return;

        ATMManager.openATMMenu(player);
    }

    /**
     * ATM 입력 대기 상태인 플레이어의 채팅을 가로채어 입출금 처리
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!ATMManager.hasState(uuid)) {
            return;
        }

        // ATM 상태일 경우 일반 채팅 브로드캐스트 차단
        event.setCancelled(true);

        String rawMsg = PlainTextComponentSerializer.plainText().serialize(event.message());

        // 마인크래프트 인벤토리/경제 조작은 메인 스레드에서 안전하게 실행
        Bukkit.getScheduler().runTask(plugin, () -> {
            ATMManager.handleChatInput(player, rawMsg);
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        ATMManager.clearState(event.getPlayer().getUniqueId());
        clickCooldown.remove(event.getPlayer().getUniqueId());
    }
}
