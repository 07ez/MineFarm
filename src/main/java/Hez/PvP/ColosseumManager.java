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

        plugin.getLogger().info(String.format("콜로세움 구역 로드 완료 - 마을월드: %s, X: %d ~ %d, Z: %d ~ %d",
                townWorldName, minX, maxX, minZ, maxZ));
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

        // 2. 현재 콜로세움 안에 있는 모든 플레이어 닉네임 목록 수집
        List<Player> onlinePlayersInArena = getInsideOnlinePlayers();
        String playerNames = onlinePlayersInArena.stream()
                .map(Player::getName)
                .collect(Collectors.joining(", "));

        // 3. 들어온 플레이어 본인에게 서브타이틀 표시
        // 메인 타이틀: §6⚔ 콜로세움 ⚔, 서브 타이틀: 입장한플레이어 : 홍길동, 김철수
        String mainTitle = "§6⚔ 콜로세움 ⚔";
        String subTitle = "§e입장한플레이어 : §f" + (playerNames.isEmpty() ? player.getName() : playerNames);
        player.sendTitle(mainTitle, subTitle, 10, 60, 20);

        try {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.2f);
        } catch (Exception ignored) {
        }
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

        // 마을 월드에 접속 중인 플레이어 전체에게 채팅 공지
        String leaveMessage = "§6[콜로세움] §f" + player.getName() + "님이 콜로세움을 나갔습니다";
        broadcastToTownWorld(leaveMessage);
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
        insidePlayers.clear();
        playerPos1Selection.clear();
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

    // Getters & Setters
    public int getMinX() { return minX; }
    public int getMaxX() { return maxX; }
    public int getMinZ() { return minZ; }
    public int getMaxZ() { return maxZ; }
    public String getTownWorldName() { return townWorldName; }
    public Set<UUID> getInsidePlayers() { return insidePlayers; }
}
