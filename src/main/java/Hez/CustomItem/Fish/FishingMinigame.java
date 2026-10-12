package Hez.CustomItem.Fish;

import Hez.MineFarm.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Color;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 낚시 리듬게임 미니게임 시스템
 * 게이지 바가 우측으로 이동하면서 초록색 구간에 들어왔을 때 클릭하면 낚시 성공!
 */
public class FishingMinigame {

    private static final Map<UUID, GameSession> activeSessions = new ConcurrentHashMap<>();

    // 게이지 바 총 길이
    public static final int BAR_LENGTH = 30;

    public record GameSession(
            Player player,
            ItemStack rewardItem,
            int greenStart,
            int greenEnd,
            int cursorPosition,
            BukkitTask task
    ) {}

    /**
     * 미니게임 시작
     * @param player 낚시 중인 플레이어
     * @param rewardItem 낚았을 때 지급할 보상 아이템 (물고기 또는 쓰레기)
     */
    public static void start(Player player, ItemStack rewardItem) {
        // 이미 진행 중인 세션이 있다면 취소
        stop(player.getUniqueId());

        // 초록색 목표 구간 설정 (난이도: 중간 12~18 사이 약 7칸)
        int greenLength = 7;
        int greenStart = 11;
        int greenEnd = greenStart + greenLength - 1;

        final int[] cursor = {0};

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    stop(player.getUniqueId());
                    cancel();
                    return;
                }

                // 끝까지 도달할 때까지 클릭하지 못하면 실패!
                if (cursor[0] >= BAR_LENGTH) {
                    onFail(player);
                    cancel();
                    return;
                }

                // 타이틀 렌더링
                renderTitle(player, cursor[0], greenStart, greenEnd);

                // 커서 1칸 전진
                cursor[0]++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 2L); // 2틱(0.1초)마다 1칸씩 전진

        activeSessions.put(player.getUniqueId(), new GameSession(player, rewardItem, greenStart, greenEnd, 0, task));
    }

    /**
     * 플레이어 클릭 시 판정
     * @return 미니게임이 진행 중이었는지 여부
     */
    public static boolean handleClick(Player player) {
        GameSession session = activeSessions.remove(player.getUniqueId());
        if (session == null) return false;

        // 태스크 중지
        session.task().cancel();

        // 클릭 시점의 커서 위치는 세션의 렌더링 기준 위치
        // (현재 커서 위치 확인을 위해 직전 렌더링 시점의 위치 계산)
        int cursor = session.cursorPosition();

        if (cursor >= session.greenStart() && cursor <= session.greenEnd()) {
            onSuccess(player, session.rewardItem());
        } else {
            onFail(player);
        }
        return true;
    }

    /**
     * 성공 처리
     */
    private static void onSuccess(Player player, ItemStack reward) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

        String itemName = reward.getItemMeta() != null && reward.getItemMeta().hasDisplayName()
                ? reward.getItemMeta().getDisplayName()
                : reward.getType().name();

        Title successTitle = Title.title(
                Component.text("§a§l성공!"),
                Component.text("§f" + itemName + "§a을(를) 낚았습니다!"),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(1500), Duration.ofMillis(500))
        );
        player.showTitle(successTitle);

        // 아이템 인벤토리에 추가 (가득 찬 경우 드롭)
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(reward);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }

        player.sendMessage("§a[낚시] §f축하합니다! §e" + itemName + "§f 획득!");
    }

    /**
     * 실패 처리
     */
    private static void onFail(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
        player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_SPLASH, 1.0f, 0.7f);

        Title failTitle = Title.title(
                Component.text("§c§l놓쳤습니다!"),
                Component.text("§7물고기가 도망쳤습니다."),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(500))
        );
        player.showTitle(failTitle);
        player.sendMessage("§c[낚시] §7타이밍을 맞추지 못해 물고기를 놓쳤습니다...");
    }

    /**
     * 화면에 게이지 바 타이틀 표시
     */
    private static void renderTitle(Player player, int cursor, int greenStart, int greenEnd) {
        StringBuilder bar = new StringBuilder();

        for (int i = 0; i < BAR_LENGTH; i++) {
            if (i == cursor) {
                // 현재 검은색 커서 바
                bar.append("§0§l█");
            } else if (i >= greenStart && i <= greenEnd) {
                // 초록색 목표 구간
                bar.append("§a§l|");
            } else {
                // 일반 배경 구간
                bar.append("§7|");
            }
        }

        Title title = Title.title(
                Component.text(bar.toString()),
                Component.text("§e[클릭!] §f검은색 바가 §a초록색 구간§f에 왔을 때 클릭하세요!"),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(300), Duration.ZERO)
        );
        player.showTitle(title);

        // 현재 위치 세션에 업데이트
        GameSession old = activeSessions.get(player.getUniqueId());
        if (old != null) {
            activeSessions.put(player.getUniqueId(), new GameSession(old.player(), old.rewardItem(), old.greenStart(), old.greenEnd(), cursor, old.task()));
        }
    }

    public static boolean isPlaying(UUID uuid) {
        return activeSessions.containsKey(uuid);
    }

    public static void stop(UUID uuid) {
        GameSession session = activeSessions.remove(uuid);
        if (session != null && session.task() != null) {
            session.task().cancel();
        }
    }

    public static void clearAll() {
        for (GameSession session : activeSessions.values()) {
            if (session.task() != null) {
                session.task().cancel();
            }
        }
        activeSessions.clear();
    }
}
