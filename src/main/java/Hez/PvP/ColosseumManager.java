package Hez.PvP;

import Hez.MineFarm.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ColosseumManager {

    private final Main plugin;
    private File configFile;
    private FileConfiguration config;

    private String townWorldName = "world";
    private int minX = 98;
    private int maxX = 129;
    private int minZ = -227;
    private int maxZ = -203;

    // 현재 콜로세움 내부에 있는 플레이어 UUID 목록
    private final Set<UUID> insidePlayers = ConcurrentHashMap.newKeySet();

    // 관리자 위치 지정용 임시 Pos1 (UUID -> [x, z])
    private final Map<UUID, int[]> playerPos1Selection = new ConcurrentHashMap<>();

    // 직업별 선택 발판/좌표 (직업명 -> [x, y, z])
    private final Map<String, int[]> classPositions = new ConcurrentHashMap<>();

    // 플레이어별 현재 선택한 직업 (UUID -> 직업명)
    private final Map<UUID, String> selectedClasses = new ConcurrentHashMap<>();

    // 기본 직업 목록
    public static final List<String> DEFAULT_CLASSES = List.of(
            "검투사", "원펀맨", "파머", "푸드파이터", "텔레포터", "두더지"
    );

    // 액션바 지속 렌더링 태스크
    private org.bukkit.scheduler.BukkitTask actionBarTask;

    public ColosseumManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * 콜로세움 설정 파일 로드 및 초기화
     */
    public void init() {
        configFile = new File(plugin.getDataFolder(), "colosseumConfig.yml");
        if (!configFile.exists()) {
            createDefaultConfig();
        }
        loadConfig();
        startActionBarTask();
    }

    private void createDefaultConfig() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            configFile.createNewFile();
            config = YamlConfiguration.loadConfiguration(configFile);
            config.set("townWorldName", "world");
            config.set("bounds.minX", 98);
            config.set("bounds.maxX", 129);
            config.set("bounds.minZ", -227);
            config.set("bounds.maxZ", -203);

            // 6개 기본 직업 기본 좌표 샘플 생성
            int startX = 101;
            for (int i = 0; i < DEFAULT_CLASSES.size(); i++) {
                String className = DEFAULT_CLASSES.get(i);
                config.set("classes." + className + ".x", startX + (i * 3));
                config.set("classes." + className + ".y", 64);
                config.set("classes." + className + ".z", -215);
            }

            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("colosseumConfig.yml 생성 실패: " + e.getMessage());
        }
    }

    public void loadConfig() {
        if (configFile == null || !configFile.exists()) {
            createDefaultConfig();
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        this.townWorldName = config.getString("townWorldName", "world");
        int x1 = config.getInt("bounds.minX", 98);
        int x2 = config.getInt("bounds.maxX", 129);
        int z1 = config.getInt("bounds.minZ", -227);
        int z2 = config.getInt("bounds.maxZ", -203);

        this.minX = Math.min(x1, x2);
        this.maxX = Math.max(x1, x2);
        this.minZ = Math.min(z1, z2);
        this.maxZ = Math.max(z1, z2);

        // 직업별 위치 로드
        classPositions.clear();
        org.bukkit.configuration.ConfigurationSection classSec = config.getConfigurationSection("classes");
        if (classSec != null) {
            for (String className : classSec.getKeys(false)) {
                int cx = classSec.getInt(className + ".x");
                int cy = classSec.getInt(className + ".y");
                int cz = classSec.getInt(className + ".z");
                classPositions.put(className, new int[]{cx, cy, cz});
            }
        } else {
            // 섹션이 없으면 기본 직업 6종 등록
            int startX = 101;
            for (int i = 0; i < DEFAULT_CLASSES.size(); i++) {
                String className = DEFAULT_CLASSES.get(i);
                int cx = startX + (i * 3);
                int cy = 64;
                int cz = -215;
                classPositions.put(className, new int[]{cx, cy, cz});
                config.set("classes." + className + ".x", cx);
                config.set("classes." + className + ".y", cy);
                config.set("classes." + className + ".z", cz);
            }
            try {
                config.save(configFile);
            } catch (IOException ignored) {}
        }

        plugin.getLogger().info(String.format("콜로세움 구역 로드 완료 - 마을월드: %s, X: %d ~ %d, Z: %d ~ %d, 직업 수: %d개",
                townWorldName, minX, maxX, minZ, maxZ, classPositions.size()));
    }

    public void saveBounds(int x1, int z1, int x2, int z2) {
        this.minX = Math.min(x1, x2);
        this.maxX = Math.max(x1, x2);
        this.minZ = Math.min(z1, z2);
        this.maxZ = Math.max(z1, z2);

        if (config == null) {
            config = YamlConfiguration.loadConfiguration(configFile);
        }
        config.set("bounds.minX", this.minX);
        config.set("bounds.maxX", this.maxX);
        config.set("bounds.minZ", this.minZ);
        config.set("bounds.maxZ", this.maxZ);
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("콜로세움 좌표 저장 실패: " + e.getMessage());
        }
    }

    /**
     * 대상 월드가 마을 월드인지 확인 (HouseManager 설정 우선 연동)
     */
    public boolean isTownWorld(World world) {
        if (world == null) return false;
        if (plugin.getHouseManager() != null) {
            return plugin.getHouseManager().isTownWorld(world);
        }
        return world.getName().equalsIgnoreCase(townWorldName);
    }

    /**
     * 특정 위치가 콜로세움 영역 내부인지 판정 (마을 월드 대상, Y 좌표 무관, X/Z만 검사)
     */
    public boolean isInColosseum(Location location) {
        if (location == null) return false;
        if (!isTownWorld(location.getWorld())) return false;

        int x = location.getBlockX();
        int z = location.getBlockZ();

        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    /**
     * 특정 플레이어가 현재 콜로세움 내부 상태인지 여부
     */
    public boolean isInside(UUID uuid) {
        return insidePlayers.contains(uuid);
    }

    /**
     * 현재 콜로세움 내부에 있는 온라인 플레이어 목록 반환
     */
    public List<Player> getInsideOnlinePlayers() {
        return insidePlayers.stream()
                .map(Bukkit::getPlayer)
                .filter(p -> p != null && p.isOnline())
                .collect(Collectors.toList());
    }

    /**
     * 플레이어 콜로세움 입장 처리
     */
    public void handlePlayerEnter(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();

        // 중복 입장 방지
        if (!insidePlayers.add(uuid)) {
            return;
        }

        // 1. 마을 월드에 접속 중인 플레이어 전체에게 채팅 공지
        String enterMessage = "§6[콜로세움] §f" + player.getName() + "님이 콜로세움에 입장하였습니다";
        broadcastToTownWorld(enterMessage);

        // 2. 들어온 플레이어 본인에게 간단한 입장 타이틀 및 효과음
        player.sendTitle("§6⚔ 콜로세움 ⚔", "", 10, 30, 10);
        try {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.2f);
        } catch (Exception ignored) {
        }

        // 3. 콜로세움 내부 플레이어들의 액션바 즉시 갱신
        updateActionBarForInsidePlayers();
    }

    /**
     * 플레이어 콜로세움 퇴장 처리
     */
    public void handlePlayerLeave(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();

        // 콜로세움 안에 있었던 경우만 퇴장 처리
        if (!insidePlayers.remove(uuid)) {
            return;
        }

        // 액션바 즉시 비우기
        try {
            player.sendActionBar(net.kyori.adventure.text.Component.empty());
        } catch (Exception ignored) {
        }

        // 마을 월드에 접속 중인 플레이어 전체에게 채팅 공지
        String leaveMessage = "§6[콜로세움] §f" + player.getName() + "님이 콜로세움을 나갔습니다";
        broadcastToTownWorld(leaveMessage);

        // 남은 콜로세움 내부 플레이어들의 액션바 갱신
        selectedClasses.remove(uuid);
        updateActionBarForInsidePlayers();
    }

    /**
     * 플레이어 발판 위치에 따른 직업 선택 감지
     */
    public void checkClassSelection(Player player, Location loc) {
        if (player == null || loc == null) return;
        if (!isInColosseum(loc)) return;

        int px = loc.getBlockX();
        int py = loc.getBlockY();
        int pz = loc.getBlockZ();

        for (Map.Entry<String, int[]> entry : classPositions.entrySet()) {
            int[] pos = entry.getValue();
            // X, Z 일치 및 Y가 발 위치(py) 또는 바로 아래 블록(py - 1) 등 발판 허용 범위(±1)에 있는지 검사
            if (pos[0] == px && pos[2] == pz && Math.abs(pos[1] - py) <= 1) {
                String className = entry.getKey();
                String current = selectedClasses.get(player.getUniqueId());
                if (!className.equals(current)) {
                    selectedClasses.put(player.getUniqueId(), className);
                    player.sendMessage("§a[콜로세움] §f" + className + " 직업을 선택하였습니다.");
                    try {
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
                    } catch (Exception ignored) {
                    }
                    updateActionBarForInsidePlayers();
                }
                break;
            }
        }
    }

    /**
     * 특정 직업의 발판 위치를 설정 파일에 저장
     */
    public void saveClassPosition(String className, int x, int y, int z) {
        classPositions.put(className, new int[]{x, y, z});
        if (config == null) {
            config = YamlConfiguration.loadConfiguration(configFile);
        }
        config.set("classes." + className + ".x", x);
        config.set("classes." + className + ".y", y);
        config.set("classes." + className + ".z", z);
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("직업 좌표 저장 실패: " + e.getMessage());
        }
    }

    /**
     * 콜로세움 내부에 있는 플레이어들에게 액션바로 참가자 목록 및 본인 선택 직업 지속 전송
     */
    private void startActionBarTask() {
        if (actionBarTask != null && !actionBarTask.isCancelled()) {
            actionBarTask.cancel();
        }
        actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateActionBarForInsidePlayers, 10L, 20L);
    }

    public void updateActionBarForInsidePlayers() {
        List<Player> onlineInArena = getInsideOnlinePlayers();
        if (onlineInArena.isEmpty()) return;

        String playerNames = onlineInArena.stream()
                .map(Player::getName)
                .collect(Collectors.joining(", "));

        for (Player p : onlineInArena) {
            String myClass = selectedClasses.get(p.getUniqueId());
            String classInfo = (myClass != null) ? " §7| §b직업: §f" + myClass : " §7| §7(직업 미선택)";
            net.kyori.adventure.text.Component actionBarComponent =
                    net.kyori.adventure.text.Component.text("§6[콜로세움] §e참가자 : §f" + playerNames + classInfo);
            p.sendActionBar(actionBarComponent);
        }
    }

    /**
     * 마을 월드에 있는 플레이어들에게만 메시지 전송
     */
    public void broadcastToTownWorld(String message) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isTownWorld(p.getWorld())) {
                p.sendMessage(message);
            }
        }
    }

    public void cleanup() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
            actionBarTask = null;
        }
        for (Player p : getInsideOnlinePlayers()) {
            try {
                p.sendActionBar(net.kyori.adventure.text.Component.empty());
            } catch (Exception ignored) {
            }
        }
        insidePlayers.clear();
        playerPos1Selection.clear();
        selectedClasses.clear();
    }

    public void setPlayerPos1(UUID uuid, int x, int z) {
        playerPos1Selection.put(uuid, new int[]{x, z});
    }

    public int[] getPlayerPos1(UUID uuid) {
        return playerPos1Selection.get(uuid);
    }

    public void clearPlayerPos1(UUID uuid) {
        playerPos1Selection.remove(uuid);
    }

    public String getSelectedClass(UUID uuid) {
        return selectedClasses.get(uuid);
    }

    public Map<String, int[]> getClassPositions() {
        return Collections.unmodifiableMap(classPositions);
    }

    // Getters & Setters
    public int getMinX() { return minX; }
    public int getMaxX() { return maxX; }
    public int getMinZ() { return minZ; }
    public int getMaxZ() { return maxZ; }
    public String getTownWorldName() { return townWorldName; }
    public Set<UUID> getInsidePlayers() { return insidePlayers; }
    public Map<UUID, String> getSelectedClasses() { return selectedClasses; }
}
