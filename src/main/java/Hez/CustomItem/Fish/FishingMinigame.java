package Hez.CustomItem.Fish;

import Hez.MineFarm.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 체스트 GUI 기반 낚시 미니게임 (에임 트래킹 / 두더지 잡기 스타일)
 */
public class FishingMinigame {



    private static final Map<UUID, GameSession> activeSessions = new ConcurrentHashMap<>();

    public static class GameSession {
        private final Player player;
        private final ItemStack rewardItem;
        private final Inventory inventory;
        private final int targetHits;       // 목표 성공 횟수
        private final double totalSeconds;   // 총 제한 시간(초)
        private int currentHits = 0;         // 현재 성공 횟수
        private int currentFishSlot = -1;    // 현재 물고기가 위치한 슬롯
        private double remainingSeconds;     // 남은 시간
        private BukkitTask timerTask;
        private boolean finished = false;

        public GameSession(Player player, ItemStack rewardItem, int targetHits, double totalSeconds, Inventory inventory) {
            this.player = player;
            this.rewardItem = rewardItem;
            this.targetHits = targetHits;
            this.totalSeconds = totalSeconds;
            this.remainingSeconds = totalSeconds;
            this.inventory = inventory;
        }

        public Player getPlayer() { return player; }
        public ItemStack getRewardItem() { return rewardItem; }
        public Inventory getInventory() { return inventory; }
        public int getTargetHits() { return targetHits; }
        public int getCurrentHits() { return currentHits; }
        public int getCurrentFishSlot() { return currentFishSlot; }
        public double getRemainingSeconds() { return remainingSeconds; }
        public boolean isFinished() { return finished; }

        public void setFinished(boolean finished) { this.finished = finished; }
        public void setTimerTask(BukkitTask timerTask) { this.timerTask = timerTask; }
        public void incrementHits() { this.currentHits++; }
        public void decrementTime(double dt) { this.remainingSeconds = Math.max(0, this.remainingSeconds - dt); }
        public void setCurrentFishSlot(int slot) { this.currentFishSlot = slot; }

        public void cancelTask() {
            if (timerTask != null) {
                timerTask.cancel();
            }
        }
    }

    private static final net.kyori.adventure.key.Key FONT_FISHING = net.kyori.adventure.key.Key.key("minefarm", "fishing");
    private static final net.kyori.adventure.key.Key FONT_DEFAULT = net.kyori.adventure.key.Key.key("minecraft", "default");

    /**
     * 상자 상단 타이틀 Component 생성
     * - 음수 오프셋(\uEBBB) 후 커스텀 GUI(\uEAAA) 렌더링
     * - 그림자 제거: shadowColor(ShadowColor.none())
     */
    public static Component buildDynamicTitle(double remainingSeconds, int currentHits, int targetHits) {
        return Component.text()
                // 1. 음수 오프셋(\uEBBB)이 앞에 오고, 그 다음 커스텀 GUI(\uEAAA) 렌더링
                .append(Component.text("\uEBBB\uEAAA")
                        .font(FONT_DEFAULT)
                        .shadowColor(net.kyori.adventure.text.format.ShadowColor.none()))
                .build();
    }

    /**
     * 미니게임 시작 (기본 희귀도 연동)
     */
    public static void start(Player player, ItemStack rewardItem, FishRarity rarity) {
        stop(player.getUniqueId(), false);

        int targetHits = rarity.getTargetHits();
        double totalSeconds = rarity.getTimeLimitSeconds();

        // 3줄(27칸) 체스트 인벤토리 생성 (동적 타이틀 적용: 시간 및 횟수 표시)
        Component titleComponent = buildDynamicTitle(totalSeconds, 0, targetHits);
        Inventory inv = Bukkit.createInventory(player, 27, titleComponent);

        GameSession session = new GameSession(player, rewardItem, targetHits, totalSeconds, inv);
        activeSessions.put(player.getUniqueId(), session);

        // 첫 물고기 스폰 (아이템 없이 빈 상자에서 자유롭게 스폰)
        spawnFishAtRandomSlot(session);

        // 인벤토리 열기
        player.openInventory(inv);

        // 1틱(0.05초)마다 남은 시간 타이머 갱신 태스크
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || session.isFinished()) {
                    cancel();
                    return;
                }

                session.decrementTime(0.05);

                // 상단 타이틀 실시간 동적 갱신
                updateDynamicTitle(session);

                // 시간 초과 시 실패
                if (session.getRemainingSeconds() <= 0) {
                    onFail(player, "시간이 초과되어 물고기가 도망쳤습니다!");
                    cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 1L, 1L);

        session.setTimerTask(task);
    }

    /**
     * 상자 상단 타이틀 실시간 동적 갱신
     */
    private static void updateDynamicTitle(GameSession session) {
        Player player = session.getPlayer();
        if (!player.isOnline()) return;

        org.bukkit.inventory.InventoryView view = player.getOpenInventory();
        if (view.getTopInventory().equals(session.getInventory())) {
            Component newTitle = buildDynamicTitle(session.getRemainingSeconds(), session.getCurrentHits(), session.getTargetHits());
            view.setTitle(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(newTitle));
        }
    }

    /**
     * 물고기 클릭 시 처리
     */
    public static void onFishClicked(Player player) {
        GameSession session = activeSessions.get(player.getUniqueId());
        if (session == null || session.isFinished()) return;

        session.incrementHits();

        // 띵! 효과음 (성공 횟수에 따라 피치 상승)
        float pitch = 1.0f + (session.getCurrentHits() * 0.25f);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, pitch);

        // 목표 달성 시 성공!
        if (session.getCurrentHits() >= session.getTargetHits()) {
            onSuccess(player);
        } else {
            // 다른 랜덤 위치로 물고기 이동
            spawnFishAtRandomSlot(session);
            updateDynamicTitle(session);
        }
    }

    /**
     * 성공 처리
     */
    private static void onSuccess(Player player) {
        GameSession session = activeSessions.remove(player.getUniqueId());
        if (session == null) return;

        session.setFinished(true);
        session.cancelTask();
        player.closeInventory();

        ItemStack reward = session.getRewardItem();
        String itemName = reward.getItemMeta() != null && reward.getItemMeta().hasDisplayName()
                ? reward.getItemMeta().getDisplayName()
                : reward.getType().name();

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

        Title successTitle = Title.title(
                Component.text("§a§l성공!"),
                Component.text("§f" + itemName + "§a을(를) 낚았습니다!"),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(1500), Duration.ofMillis(500))
        );
        player.showTitle(successTitle);

        // 아이템 지급
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(reward);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }

        // 레시피 해금용 물고기 획득 기록
        Hez.CustomItem.RecipeUntil.recordItemAcquired(player, reward);

        player.sendMessage("§a[낚시] §f축하합니다! §e" + itemName + "§f 획득!");
    }

    /**
     * 실패 처리
     */
    public static void onFail(Player player, String reason) {
        GameSession session = activeSessions.remove(player.getUniqueId());
        if (session == null) return;

        session.setFinished(true);
        session.cancelTask();
        player.closeInventory();

        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
        player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_SPLASH, 1.0f, 0.8f);

        Title failTitle = Title.title(
                Component.text("§c§l놓쳤습니다!"),
                Component.text("§7" + reason),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(500))
        );
        player.showTitle(failTitle);
        player.sendMessage("§c[낚시] §7" + reason);
    }

    /**
     * 물고기를 전체 27칸 중 랜덤 빈 위치로 이동
     */
    private static void spawnFishAtRandomSlot(GameSession session) {
        Inventory inv = session.getInventory();

        // 기존 물고기 슬롯을 비움
        if (session.getCurrentFishSlot() != -1) {
            inv.setItem(session.getCurrentFishSlot(), null);
        }

        // 전체 0 ~ 26번 슬롯 중 기존과 다른 랜덤 슬롯 선택
        List<Integer> availableSlots = new ArrayList<>();
        for (int i = 0; i < 27; i++) {
            if (i != session.getCurrentFishSlot()) {
                availableSlots.add(i);
            }
        }

        int nextSlot = availableSlots.get(ThreadLocalRandom.current().nextInt(availableSlots.size()));
        session.setCurrentFishSlot(nextSlot);

        // 물고기 아이템 표시용 복사본
        ItemStack displayFish = session.getRewardItem().clone();
        ItemMeta meta = displayFish.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("§e§l클릭하여 잡으세요!");
            meta.setLore(lore);
            displayFish.setItemMeta(meta);
        }

        inv.setItem(nextSlot, displayFish);
    }

    public static boolean isPlaying(UUID uuid) {
        return activeSessions.containsKey(uuid);
    }

    public static GameSession getSession(UUID uuid) {
        return activeSessions.get(uuid);
    }

    public static void stop(UUID uuid, boolean notifyFail) {
        GameSession session = activeSessions.remove(uuid);
        if (session != null) {
            session.setFinished(true);
            session.cancelTask();
            if (notifyFail && session.getPlayer().isOnline()) {
                onFail(session.getPlayer(), "낚시를 중단했습니다.");
            }
        }
    }

    public static void clearAll() {
        for (GameSession session : activeSessions.values()) {
            session.setFinished(true);
            session.cancelTask();
        }
        activeSessions.clear();
    }
}
