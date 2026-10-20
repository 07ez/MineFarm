package Hez.Wilderness;

import Hez.MineFarm.Main;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class WildItemCleaner {

    private final Main plugin;
    private final WildManager wildManager;
    private BukkitTask task;
    private int remainingSeconds = 300; // 5분 = 300초

    public WildItemCleaner(Main plugin, WildManager wildManager) {
        this.plugin = plugin;
        this.wildManager = wildManager;
    }

    /**
     * 1초(20틱)마다 카운트다운하며 5분 주기로 바닥 아이템 청소
     */
    public void start() {
        stop();
        remainingSeconds = 300;

        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            remainingSeconds--;

            // 1분 전 알림
            if (remainingSeconds == 60) {
                broadcastNotification("§6[청소] §e1분 뒤 바닥에 떨어진 아이템이 모두 청소됩니다!", Sound.BLOCK_NOTE_BLOCK_PLING);
            }
            // 10초 전 알림
            else if (remainingSeconds == 10) {
                broadcastNotification("§c[청소] §f10초 뒤 바닥에 떨어진 아이템이 청소됩니다!", Sound.BLOCK_NOTE_BLOCK_BASS);
            }
            // 5초 ~ 1초 카운트다운 (선택)
            else if (remainingSeconds > 0 && remainingSeconds <= 5) {
                broadcastNotification("§c[청소] §f" + remainingSeconds + "초 뒤 아이템이 청소됩니다!", Sound.UI_BUTTON_CLICK);
            }
            // 청소 실행
            else if (remainingSeconds <= 0) {
                cleanItems();
                remainingSeconds = 300; // 다시 5분 리셋
            }
        }, 20L, 20L);
    }

    /**
     * 바닥에 떨어진 아이템 청소 실행
     */
    public int cleanItems() {
        World wildWorld = wildManager.getWildWorld();
        if (wildWorld == null) return 0;

        int count = 0;
        for (Item item : wildWorld.getEntitiesByClass(Item.class)) {
            // 특별한 보호 태그가 없으면 제거
            item.remove();
            count++;
        }

        String message = "§a[청소] §f야생 월드에서 바닥에 떨어진 아이템 §e" + count + "§f개가 청소되었습니다.";
        for (Player p : wildWorld.getPlayers()) {
            p.sendMessage(message);
            p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 0.5f);
        }

        plugin.getLogger().info("야생 월드 바닥 아이템 " + count + "개 청소 완료.");
        return count;
    }

    private void broadcastNotification(String message, Sound sound) {
        World wildWorld = wildManager.getWildWorld();
        if (wildWorld == null) return;

        for (Player p : wildWorld.getPlayers()) {
            p.sendMessage(message);
            if (sound != null) {
                p.playSound(p.getLocation(), sound, 0.8f, 1.0f);
            }
        }
    }

    public void stop() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
            task = null;
        }
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }
}
