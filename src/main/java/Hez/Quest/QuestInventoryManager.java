package Hez.Quest;

import Hez.MineFarm.Main;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 퀘스트 창 오픈 시 플레이어의 인벤토리(보관함 36칸)를 안전하게 백업 및 클리어하고,
 * 퀘스트 창 종료/비정상 종료/서버 리로드/크래시 시 100% 원상복구하는 매니저.
 */
public class QuestInventoryManager implements Listener {

    private static QuestInventoryManager instance;

    // 메모리 캐시 (UUID -> 보관함 36칸 복사본)
    private final Map<UUID, ItemStack[]> backupStorageMap = new ConcurrentHashMap<>();
    // 메모리 캐시 (UUID -> 커서 아이템 복사본)
    private final Map<UUID, ItemStack> backupCursorMap = new ConcurrentHashMap<>();
    // 현재 퀘스트 창을 열고 있는 플레이어 집합
    private final Set<UUID> activeQuestPlayers = ConcurrentHashMap.newKeySet();

    // 서버 크래시/정전 대비 디스크 영구 저장 파일 (quest_inventory_backup.yml)
    private final File backupFile;
    private FileConfiguration backupConfig;
    private final Object fileLock = new Object();

    public QuestInventoryManager() {
        instance = this;
        File dataFolder = Main.getInstance().getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.backupFile = new File(dataFolder, "quest_inventory_backup.yml");
        loadBackupFile();
    }

    public static QuestInventoryManager getInstance() {
        if (instance == null) {
            instance = new QuestInventoryManager();
        }
        return instance;
    }

    private void loadBackupFile() {
        synchronized (fileLock) {
            if (!backupFile.exists()) {
                try {
                    backupFile.createNewFile();
                } catch (IOException e) {
                    Main.getInstance().getLogger().severe("quest_inventory_backup.yml 생성 실패: " + e.getMessage());
                }
            }
            this.backupConfig = YamlConfiguration.loadConfiguration(backupFile);
        }
    }

    private void saveBackupFile() {
        synchronized (fileLock) {
            try {
                backupConfig.save(backupFile);
            } catch (IOException e) {
                Main.getInstance().getLogger().severe("quest_inventory_backup.yml 저장 실패: " + e.getMessage());
            }
        }
    }

    /**
     * 특정 플레이어의 백업 데이터가 메모리 또는 디스크에 이미 존재하는지 확인
     */
    public boolean hasBackup(UUID uuid) {
        if (backupStorageMap.containsKey(uuid)) {
            return true;
        }
        synchronized (fileLock) {
            return backupConfig.contains("backups." + uuid.toString());
        }
    }

    /**
     * 플레이어가 현재 퀘스트 창을 보고 있는지 확인
     */
    public boolean isQuestOpen(UUID uuid) {
        return activeQuestPlayers.contains(uuid);
    }

    /**
     * [핵심] 인벤토리 백업 및 비우기 (클리어)
     * - 이미 백업이 존재하면 절대로 빈 인벤토리로 덮어쓰지 않음 (증발 원천 차단)
     * - 깊은 복사(Deep Clone) 후 메모리와 디스크 파일에 동시 기록
     * - 갑옷 및 왼손을 제외한 순수 인벤토리(보관함 36칸)만 비움
     */
    public void backupAndClear(Player player) {
        UUID uuid = player.getUniqueId();
        activeQuestPlayers.add(uuid);

        // ★ 이중 호출 방지: 이미 백업이 되어 있다면 기존 백업 보존하고 인벤토리만 비움
        if (hasBackup(uuid)) {
            player.setItemOnCursor(null);
            player.getInventory().setStorageContents(new ItemStack[36]);
            player.updateInventory();
            return;
        }

        // 1. 플레이어 보관함 36칸 (0~35) 깊은 복사
        ItemStack[] originalStorage = player.getInventory().getStorageContents();
        ItemStack[] clonedStorage = new ItemStack[36];
        for (int i = 0; i < Math.min(originalStorage.length, 36); i++) {
            ItemStack item = originalStorage[i];
            clonedStorage[i] = (item != null && !item.getType().isAir()) ? item.clone() : null;
        }

        // 2. 커서에 쥐고 있는 아이템이 있다면 백업
        ItemStack cursor = player.getItemOnCursor();
        ItemStack clonedCursor = (cursor != null && !cursor.getType().isAir()) ? cursor.clone() : null;

        // 3. 메모리 캐시 저장
        backupStorageMap.put(uuid, clonedStorage);
        if (clonedCursor != null) {
            backupCursorMap.put(uuid, clonedCursor);
        }

        // 4. 디스크 파일 즉시 저장 (서버 크래시/강종 대비)
        synchronized (fileLock) {
            String path = "backups." + uuid;
            backupConfig.set(path + ".storage", Arrays.asList(clonedStorage));
            if (clonedCursor != null) {
                backupConfig.set(path + ".cursor", clonedCursor);
            }
            saveBackupFile();
        }

        // 5. 인벤토리 클리어 (보관함 36칸 및 커서)
        player.setItemOnCursor(null);
        player.getInventory().setStorageContents(new ItemStack[36]);
        player.updateInventory();
    }

    /**
     * [핵심] 인벤토리 원상복구
     * - 메모리 또는 디스크에서 백업된 아이템을 가져와 플레이어의 인벤토리에 복원
     * - 복원 완료 후 메모리와 디스크에서 안전하게 제거
     */
    public void restore(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        activeQuestPlayers.remove(uuid);

        ItemStack[] storage = backupStorageMap.remove(uuid);
        ItemStack cursor = backupCursorMap.remove(uuid);

        // 메모리에 없으면 디스크에서 로드 시도 (서버 재시작 등)
        if (storage == null) {
            storage = loadStorageFromDisk(uuid);
            cursor = loadCursorFromDisk(uuid);
        }

        // 복원 대상이 존재하는 경우 복원 진행
        if (storage != null) {
            player.getInventory().setStorageContents(storage);
            if (cursor != null) {
                player.setItemOnCursor(cursor);
            }
            player.updateInventory();
        }

        // 디스크에서 백업 데이터 영구 제거
        synchronized (fileLock) {
            String path = "backups." + uuid;
            if (backupConfig.contains(path)) {
                backupConfig.set(path, null);
                saveBackupFile();
            }
        }
    }

    /**
     * 디스크에서 보관함 아이템 배열 불러오기
     */
    private ItemStack[] loadStorageFromDisk(UUID uuid) {
        synchronized (fileLock) {
            String path = "backups." + uuid + ".storage";
            if (!backupConfig.contains(path)) {
                return null;
            }
            List<?> list = backupConfig.getList(path);
            if (list == null) return null;

            ItemStack[] items = new ItemStack[36];
            for (int i = 0; i < Math.min(list.size(), 36); i++) {
                Object obj = list.get(i);
                if (obj instanceof ItemStack is) {
                    items[i] = is;
                } else if (obj instanceof Map<?, ?> map) {
                    try {
                        items[i] = ItemStack.deserialize((Map<String, Object>) map);
                    } catch (Exception ignored) {
                    }
                }
            }
            return items;
        }
    }

    /**
     * 디스크에서 커서 아이템 불러오기
     */
    private ItemStack loadCursorFromDisk(UUID uuid) {
        synchronized (fileLock) {
            String path = "backups." + uuid + ".cursor";
            return backupConfig.getItemStack(path);
        }
    }

    /**
     * 서버 종료 / 리로드 시 모든 플레이어의 인벤토리를 안전하게 일괄 복원
     */
    public void restoreAll() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (hasBackup(online.getUniqueId())) {
                if (online.getOpenInventory().getTopInventory().getHolder() instanceof QuestMenu) {
                    online.closeInventory();
                }
                restore(online);
            }
        }
    }

    // ==========================================
    // 이벤트 리스너: 예외 상황 및 안전 장치
    // ==========================================

    /**
     * 퀘스트 창 닫힐 때 인벤토리 자동 복구
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof QuestMenu) {
            Player player = (Player) event.getPlayer();
            restore(player);
        }
    }

    /**
     * 플레이어 퇴장/튕김 시 즉시 인벤토리 복구
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (hasBackup(player.getUniqueId())) {
            restore(player);
        }
    }

    /**
     * 플레이어 접속 시: 만약 이전 세션에서 비정상 종료(크래시 등)로 디스크에 백업이 남아있다면 즉시 복구!
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (hasBackup(player.getUniqueId())) {
            restore(player);
            player.sendMessage("§e[퀘스트] 이전 세션에서 보관 중이던 인벤토리 아이템이 안전하게 복구되었습니다.");
        }
    }

    /**
     * 퀘스트 창을 열고 있는 도중 플레이어 사망 시:
     * 원래 아이템을 즉시 복원하여 사망 시 아이템이 유실되지 않도록 보장
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (hasBackup(player.getUniqueId())) {
            restore(player);
        }
    }

    /**
     * 퀘스트 창 열려있는 동안 인벤토리 조작 방지 (상단/하단 모두)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof QuestMenu) {
            event.setCancelled(true);
            return;
        }
        if (isQuestOpen(event.getWhoClicked().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * 퀘스트 창 열려있는 동안 인벤토리 드래그 방지
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof QuestMenu) {
            event.setCancelled(true);
            return;
        }
        if (isQuestOpen(event.getWhoClicked().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * 퀘스트 창 열려있는 동안 바닥 아이템 줍기 방지 (백업 복원 시 덮어쓰기 방지)
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (isQuestOpen(player.getUniqueId()) || hasBackup(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * 퀘스트 창 열려있는 동안 Q키 등으로 아이템 버리기 방지
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (isQuestOpen(player.getUniqueId()) || hasBackup(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * 퀘스트 창 열려있는 동안 F키 스왑 방지
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (isQuestOpen(player.getUniqueId()) || hasBackup(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
