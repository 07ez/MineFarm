package Hez.PvP;

import Hez.MineFarm.Main;
import Hez.PvP.Class.ColosseumClassType;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ColosseumManager {

    private final Main plugin;
    private File configFile;
    private FileConfiguration config;

    // 마을 월드 및 콜로세움 대기실 좌표
    private String townWorldName = "world";
    private int minX = 98;
    private int maxX = 129;
    private int minZ = -227;
    private int maxZ = -203;

    // 콜로세움 실제 전투 평지(Flat) 월드
    public static final String DEFAULT_ARENA_WORLD_NAME = "colosseum";
    private String arenaWorldName = DEFAULT_ARENA_WORLD_NAME;
    private World arenaWorld;
    private Location arenaSpawnLocation;

    // 현재 콜로세움 내부에 있는 플레이어 UUID 목록
    private final Set<UUID> insidePlayers = ConcurrentHashMap.newKeySet();

    // 관리자 위치 지정용 임시 Pos1 (UUID -> [x, z])
    private final Map<UUID, int[]> playerPos1Selection = new ConcurrentHashMap<>();

    // 직업별 선택 발판/좌표 (직업명 -> [x, z])
    private final Map<String, int[]> classPositions = new ConcurrentHashMap<>();

    // 플레이어별 현재 선택한 직업 (UUID -> 직업명)
    private final Map<UUID, String> selectedClasses = new ConcurrentHashMap<>();

    // 기본 직업 목록
    public static final List<String> DEFAULT_CLASSES = List.of(
            "검투사", "원펀맨", "파머", "푸드파이터", "텔레포터", "두더지"
    );

    // 팀별 선택 구역 (팀명 -> TeamArea)
    private final Map<String, TeamArea> teamAreas = new ConcurrentHashMap<>();

    // 플레이어별 현재 선택한 팀 (UUID -> 팀명)
    private final Map<UUID, String> selectedTeams = new ConcurrentHashMap<>();

    // 관리자 팀 구역 지정용 임시 Pos1 (UUID -> [x, z])
    private final Map<UUID, int[]> playerTeamPos1Selection = new ConcurrentHashMap<>();

    // 기본 팀 목록 (green, red, blue, yellow, spectator)
    public static final List<String> DEFAULT_TEAMS = List.of(
            "green", "red", "blue", "yellow", "spectator"
    );

    // 준비 완료한 플레이어 UUID 목록 (직업+팀 선택 후 웅크리기)
    private final Set<UUID> readyPlayers = ConcurrentHashMap.newKeySet();

    // 시작 카운트다운 태스크 및 진행 상태
    private org.bukkit.scheduler.BukkitTask countdownTask = null;
    private int countdownSeconds = 3;
    private boolean matchInProgress = false;

    // 경기 참가자 원래 인벤토리 및 상태 백업 (UUID -> PlayerBackupData)
    private final Map<UUID, PlayerBackupData> inventoryBackups = new ConcurrentHashMap<>();

    public static class PlayerBackupData {
        private final ItemStack[] contents;
        private final ItemStack[] armorContents;
        private final ItemStack[] extraContents;
        private final double health;
        private final int foodLevel;
        private final float saturation;
        private final int level;
        private final float exp;
        private final Collection<PotionEffect> activeEffects;
        private final GameMode gameMode;

        public PlayerBackupData(Player p) {
            this.contents = cloneItemArray(p.getInventory().getStorageContents());
            this.armorContents = cloneItemArray(p.getInventory().getArmorContents());
            this.extraContents = cloneItemArray(p.getInventory().getExtraContents());
            this.health = p.getHealth();
            this.foodLevel = p.getFoodLevel();
            this.saturation = p.getSaturation();
            this.level = p.getLevel();
            this.exp = p.getExp();
            this.activeEffects = new ArrayList<>(p.getActivePotionEffects());
            this.gameMode = p.getGameMode();
        }

        public void restore(Player p) {
            if (p == null || !p.isOnline()) return;
            p.getInventory().clear();
            p.getInventory().setStorageContents(cloneItemArray(this.contents));
            p.getInventory().setArmorContents(cloneItemArray(this.armorContents));
            p.getInventory().setExtraContents(cloneItemArray(this.extraContents));
            for (PotionEffect effect : p.getActivePotionEffects()) {
                p.removePotionEffect(effect.getType());
            }
            for (PotionEffect effect : this.activeEffects) {
                p.addPotionEffect(effect);
            }
            p.setGameMode(this.gameMode);
            p.setLevel(this.level);
            p.setExp(this.exp);
            p.setFoodLevel(this.foodLevel);
            p.setSaturation(this.saturation);
            try {
                p.setHealth(Math.min(this.health, p.getMaxHealth()));
            } catch (Exception ignored) {}
        }

        private static ItemStack[] cloneItemArray(ItemStack[] source) {
            if (source == null) return new ItemStack[0];
            ItemStack[] copy = new ItemStack[source.length];
            for (int i = 0; i < source.length; i++) {
                copy[i] = source[i] != null ? source[i].clone() : null;
            }
            return copy;
        }
    }

    public static class TeamArea {
        private final int minX;
        private final int maxX;
        private final int minZ;
        private final int maxZ;

        public TeamArea(int x1, int z1, int x2, int z2) {
            this.minX = Math.min(x1, x2);
            this.maxX = Math.max(x1, x2);
            this.minZ = Math.min(z1, z2);
            this.maxZ = Math.max(z1, z2);
        }

        public boolean contains(int x, int z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }

        public int getMinX() { return minX; }
        public int getMaxX() { return maxX; }
        public int getMinZ() { return minZ; }
        public int getMaxZ() { return maxZ; }
    }

    // 액션바 지속 렌더링 태스크
    private org.bukkit.scheduler.BukkitTask actionBarTask;

    // 경기장(메인 및 테마 맵) 템플릿 관리자
    private final ArenaTemplateManager templateManager;

    public ColosseumManager(Main plugin) {
        this.plugin = plugin;
        this.templateManager = new ArenaTemplateManager(plugin, this);
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
        initArenaWorld();
        this.templateManager.init();
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
            config.set("arenaWorldName", DEFAULT_ARENA_WORLD_NAME);
            config.set("bounds.minX", 98);
            config.set("bounds.maxX", 129);
            config.set("bounds.minZ", -227);
            config.set("bounds.maxZ", -203);

            // 기본 결투장 스폰 좌표 (Flat 월드 y=4)
            config.set("arenaSpawn.x", 0.5);
            config.set("arenaSpawn.y", 4.0);
            config.set("arenaSpawn.z", 0.5);
            config.set("arenaSpawn.yaw", 0.0);
            config.set("arenaSpawn.pitch", 0.0);

            // 6개 기본 직업 기본 좌표 샘플 생성 (X, Z만 사용)
            int startX = 101;
            for (int i = 0; i < DEFAULT_CLASSES.size(); i++) {
                String className = DEFAULT_CLASSES.get(i);
                config.set("classes." + className + ".x", startX + (i * 3));
                config.set("classes." + className + ".z", -215);
            }

            // 기본 팀 구역 샘플 생성 (minX ~ maxX, minZ ~ maxZ)
            int teamStartX = 100;
            for (int i = 0; i < DEFAULT_TEAMS.size(); i++) {
                String teamName = DEFAULT_TEAMS.get(i);
                int sx = teamStartX + (i * 5);
                config.set("teams." + teamName + ".minX", sx);
                config.set("teams." + teamName + ".maxX", sx + 3);
                config.set("teams." + teamName + ".minZ", -225);
                config.set("teams." + teamName + ".maxZ", -221);
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
        this.arenaWorldName = config.getString("arenaWorldName", DEFAULT_ARENA_WORLD_NAME);
        int x1 = config.getInt("bounds.minX", 98);
        int x2 = config.getInt("bounds.maxX", 129);
        int z1 = config.getInt("bounds.minZ", -227);
        int z2 = config.getInt("bounds.maxZ", -203);

        this.minX = Math.min(x1, x2);
        this.maxX = Math.max(x1, x2);
        this.minZ = Math.min(z1, z2);
        this.maxZ = Math.max(z1, z2);

        // 직업별 위치 로드 (X, Z만)
        classPositions.clear();
        org.bukkit.configuration.ConfigurationSection classSec = config.getConfigurationSection("classes");
        if (classSec != null) {
            for (String className : classSec.getKeys(false)) {
                int cx = classSec.getInt(className + ".x");
                int cz = classSec.getInt(className + ".z");
                classPositions.put(className, new int[]{cx, cz});
            }
        } else {
            // 섹션이 없으면 기본 직업 6종 등록
            int startX = 101;
            for (int i = 0; i < DEFAULT_CLASSES.size(); i++) {
                String className = DEFAULT_CLASSES.get(i);
                int cx = startX + (i * 3);
                int cz = -215;
                classPositions.put(className, new int[]{cx, cz});
                config.set("classes." + className + ".x", cx);
                config.set("classes." + className + ".z", cz);
            }
            try {
                config.save(configFile);
            } catch (IOException ignored) {}
        }

        // 팀별 구역 로드 (minX ~ maxX, minZ ~ maxZ)
        teamAreas.clear();
        org.bukkit.configuration.ConfigurationSection teamSec = config.getConfigurationSection("teams");
        if (teamSec != null) {
            for (String teamName : teamSec.getKeys(false)) {
                int tMinX = teamSec.getInt(teamName + ".minX");
                int tMaxX = teamSec.getInt(teamName + ".maxX");
                int tMinZ = teamSec.getInt(teamName + ".minZ");
                int tMaxZ = teamSec.getInt(teamName + ".maxZ");
                teamAreas.put(teamName.toLowerCase(), new TeamArea(tMinX, tMinZ, tMaxX, tMaxZ));
            }
        } else {
            int teamStartX = 100;
            for (int i = 0; i < DEFAULT_TEAMS.size(); i++) {
                String teamName = DEFAULT_TEAMS.get(i);
                int sx = teamStartX + (i * 5);
                teamAreas.put(teamName.toLowerCase(), new TeamArea(sx, -225, sx + 3, -221));
                config.set("teams." + teamName + ".minX", sx);
                config.set("teams." + teamName + ".maxX", sx + 3);
                config.set("teams." + teamName + ".minZ", -225);
                config.set("teams." + teamName + ".maxZ", -221);
            }
            try {
                config.save(configFile);
            } catch (IOException ignored) {}
        }

        plugin.getLogger().info(String.format("콜로세움 구역 로드 완료 - 마을월드: %s, X: %d ~ %d, Z: %d ~ %d, 직업: %d개, 팀 구역: %d개",
                townWorldName, minX, maxX, minZ, maxZ, classPositions.size(), teamAreas.size()));
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
     * 결투장 평지(Flat) 월드 생성 및 초기화
     */
    public void initArenaWorld() {
        this.arenaWorld = Bukkit.getWorld(arenaWorldName);
        if (this.arenaWorld == null) {
            WorldCreator creator = new WorldCreator(arenaWorldName);
            creator.environment(World.Environment.NORMAL);
            creator.type(WorldType.FLAT);
            creator.generateStructures(false);
            this.arenaWorld = Bukkit.createWorld(creator);
        }

        if (this.arenaWorld != null) {
            this.arenaWorld.setGameRule(GameRules.SPAWN_MOBS, false);
            this.arenaWorld.setGameRule(GameRules.ADVANCE_TIME, false);
            this.arenaWorld.setGameRule(GameRules.ADVANCE_WEATHER, false);
            this.arenaWorld.setGameRule(GameRules.RANDOM_TICK_SPEED, 0); // 파머 능력: 자연 성장 방지
            this.arenaWorld.setTime(6000L); // 항상 맑은 낮
            this.arenaWorld.setStorm(false);
            this.arenaWorld.setDifficulty(Difficulty.NORMAL);
            this.arenaWorld.setPVP(true);

            loadArenaSpawnLocation();
            plugin.getLogger().info("콜로세움 결투장 평지 월드('" + arenaWorldName + "')가 성공적으로 로드되었습니다.");
        } else {
            plugin.getLogger().severe("콜로세움 결투장 월드('" + arenaWorldName + "')를 생성하거나 불러오지 못했습니다!");
        }
    }

    private void loadArenaSpawnLocation() {
        if (config == null || arenaWorld == null) return;
        if (config.contains("arenaSpawn.x")) {
            double x = config.getDouble("arenaSpawn.x");
            double y = config.getDouble("arenaSpawn.y");
            double z = config.getDouble("arenaSpawn.z");
            float yaw = (float) config.getDouble("arenaSpawn.yaw", 0.0);
            float pitch = (float) config.getDouble("arenaSpawn.pitch", 0.0);
            this.arenaSpawnLocation = new Location(arenaWorld, x, y, z, yaw, pitch);
        } else {
            this.arenaSpawnLocation = new Location(arenaWorld, 0.5, 4.0, 0.5, 0.0f, 0.0f);
        }
    }

    public void setArenaSpawnLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        this.arenaSpawnLocation = loc.clone();
        if (config == null) {
            config = YamlConfiguration.loadConfiguration(configFile);
        }
        config.set("arenaSpawn.x", loc.getX());
        config.set("arenaSpawn.y", loc.getY());
        config.set("arenaSpawn.z", loc.getZ());
        config.set("arenaSpawn.yaw", loc.getYaw());
        config.set("arenaSpawn.pitch", loc.getPitch());
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("결투장 스폰 저장 실패: " + e.getMessage());
        }
    }

    public Location getArenaSpawnLocation() {
        if (config != null && config.contains("arenaSpawn.x") && arenaWorld != null) {
            loadArenaSpawnLocation();
        }
        if (arenaSpawnLocation == null && arenaWorld != null) {
            arenaSpawnLocation = new Location(arenaWorld, 0.5, 4.0, 0.5, 0.0f, 0.0f);
        }
        return arenaSpawnLocation;
    }

    public boolean teleportToArena(Player player) {
        if (player == null) return false;
        if (arenaWorld == null) {
            initArenaWorld();
        }
        if (arenaWorld == null) {
            player.sendMessage("§c[콜로세움] 결투장 월드를 불러올 수 없습니다!");
            return false;
        }
        Location target = getArenaSpawnLocation();
        player.teleport(target);
        player.sendMessage("§a[콜로세움] 결투장 평지 월드(" + arenaWorldName + ")로 이동했습니다.");
        try {
            player.playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        } catch (Exception ignored) {
        }
        return true;
    }

    public boolean isArenaWorld(World world) {
        if (world == null) return false;
        return world.getName().equalsIgnoreCase(arenaWorldName);
    }

    public World getArenaWorld() {
        return arenaWorld;
    }

    public String getArenaWorldName() {
        return arenaWorldName;
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
        checkCountdownStatus();
    }

    /**
     * 플레이어 콜로세움 퇴장 처리
     */
    public void handlePlayerLeave(Player player) {
        if (player == null) return;
        // 게임 진행 중일 때는 아레나로 이동한 것이므로 퇴장 및 데이터 삭제 처리하지 않음!
        if (matchInProgress) return;
        UUID uuid = player.getUniqueId();

        // 콜로세움 안에 있었던 경우만 퇴장 처리
        if (!insidePlayers.remove(uuid)) {
            return;
        }

        // 액션바 즉시 비우기 및 직업 특수 효과(원펀맨 힘) 제거
        try {
            player.sendActionBar(net.kyori.adventure.text.Component.empty());
            player.removePotionEffect(PotionEffectType.STRENGTH);
        } catch (Exception ignored) {
        }

        // 마을 월드에 접속 중인 플레이어 전체에게 채팅 공지
        String leaveMessage = "§6[콜로세움] §f" + player.getName() + "님이 콜로세움을 나갔습니다";
        broadcastToTownWorld(leaveMessage);

        // 남은 콜로세움 내부 플레이어들의 액션바 갱신
        selectedClasses.remove(uuid);
        selectedTeams.remove(uuid);
        readyPlayers.remove(uuid);

        // 혹시 경기 중 인벤토리 백업이 남아있다면 복원
        restorePlayer(player);

        updateActionBarForInsidePlayers();
        checkCountdownStatus();
    }

    /**
     * 플레이어 발판 위치에 따른 직업 선택 감지 (X, Z 좌표만 일치하면 선택, 효과는 지급하지 않음)
     */
    public void checkClassSelection(Player player, Location loc) {
        if (player == null || loc == null) return;
        if (!isInColosseum(loc)) return;

        int px = loc.getBlockX();
        int pz = loc.getBlockZ();

        for (Map.Entry<String, int[]> entry : classPositions.entrySet()) {
            int[] pos = entry.getValue();
            // X, Z 일치 검사 (Y 좌표는 무관)
            if (pos[0] == px && pos[1] == pz) {
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
     * 플레이어 구역(범위: minX~maxX, minZ~maxZ) 진입 및 이탈에 따른 팀 선택/해제 감지
     * - 구역에 있으면 팀 선택
     * - 구역을 벗어나면 팀 자동 해제 및 준비 상태 자동 해제!
     */
    public void checkTeamSelection(Player player, Location loc) {
        if (player == null || loc == null) return;
        if (!isInColosseum(loc)) return;

        int px = loc.getBlockX();
        int pz = loc.getBlockZ();

        String matchedTeam = null;
        for (Map.Entry<String, TeamArea> entry : teamAreas.entrySet()) {
            if (entry.getValue().contains(px, pz)) {
                matchedTeam = entry.getKey();
                break;
            }
        }

        UUID uuid = player.getUniqueId();
        String currentTeam = selectedTeams.get(uuid);

        if (matchedTeam != null) {
            // 팀 구역 내부 진입 또는 다른 팀 구역으로 변경
            if (!matchedTeam.equalsIgnoreCase(currentTeam)) {
                selectedTeams.put(uuid, matchedTeam);
                readyPlayers.remove(uuid); // 팀이 바뀌면 기존 준비 상태 해제

                String formatted = formatTeamName(matchedTeam);
                if ("spectator".equalsIgnoreCase(matchedTeam)) {
                    player.sendMessage("§7[콜로세움] §f관전자를 선택하였습니다.");
                } else {
                    player.sendMessage("§a[콜로세움] §f" + formatted + "§f을(를) 선택하였습니다.");
                }
                try {
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
                } catch (Exception ignored) {
                }
                updateActionBarForInsidePlayers();
                checkCountdownStatus();
            }
        } else {
            // 어떤 팀 구역에도 속하지 않음 -> 팀 구역을 벗어났으므로 팀 해제 및 준비 해제!
            if (currentTeam != null) {
                selectedTeams.remove(uuid);
                readyPlayers.remove(uuid);
                player.sendMessage("§c[콜로세움] §f팀 구역을 벗어나 팀 선택 및 준비가 해제되었습니다.");
                try {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
                } catch (Exception ignored) {
                }
                updateActionBarForInsidePlayers();
                checkCountdownStatus();
            }
        }
    }

    /**
     * 웅크리기(Shift) 시 준비 상태 토글
     * (직업과 팀이 모두 선택된 상태에서만 가능하며, 관전자는 팀만 선택해도 준비 가능. 팀 구역을 나가면 준비도 자동 해제됨)
     */
    public boolean toggleReady(Player player) {
        if (player == null) return false;
        UUID uuid = player.getUniqueId();

        if (!isInColosseum(player.getLocation())) return false;
        if (matchInProgress) {
            player.sendMessage("§c[콜로세움] 이미 결투가 진행 중입니다!");
            return false;
        }

        String myClass = selectedClasses.get(uuid);
        String myTeam = selectedTeams.get(uuid);

        boolean isSpectator = "spectator".equalsIgnoreCase(myTeam);

        if (myTeam == null || (!isSpectator && myClass == null)) {
            if (isSpectator) {
                player.sendMessage("§c[콜로세움] §f팀을 먼저 선택한 상태에서 웅크려야 준비할 수 있습니다!");
            } else {
                player.sendMessage("§c[콜로세움] §f직업과 팀을 모두 선택한 상태에서 웅크려야 준비할 수 있습니다!");
            }
            try {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1.0f);
            } catch (Exception ignored) {
            }
            return false;
        }

        if (readyPlayers.contains(uuid)) {
            readyPlayers.remove(uuid);
            player.sendMessage("§e[콜로세움] §f준비를 취소하였습니다.");
            try {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 0.8f);
            } catch (Exception ignored) {
            }
        } else {
            readyPlayers.add(uuid);
            player.sendMessage("§a[콜로세움] §f준비 완료! (전투 시작 대기 중)");
            try {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.5f);
            } catch (Exception ignored) {
            }
        }
        updateActionBarForInsidePlayers();
        checkCountdownStatus();
        return true;
    }

    /**
     * 게임 시작 조건 검사:
     * 1. 경기가 이미 진행 중이 아니어야 함
     * 2. 대기실에 2명 이상의 플레이어가 있어야 함
     * 3. 대기실에 들어온 모든 플레이어가 레디(Ready) 상태여야 함
     * 4. 관전자(spectator)를 제외한 실전 참여 팀이 2개 이상이어야 함 (예: Red + Spectator 는 팀 1개라 시작 안 됨)
     */
    public boolean canStartGame() {
        if (matchInProgress) return false;

        List<Player> inside = getInsideOnlinePlayers();
        if (inside.size() < 2) return false;

        // 대기실의 모든 플레이어가 준비 완료 상태인지 검사
        for (Player p : inside) {
            if (!readyPlayers.contains(p.getUniqueId())) {
                return false;
            }
        }

        // 관전자(spectator)를 제외한 실전 팀 수 계산
        Set<String> fightingTeams = new HashSet<>();
        for (Player p : inside) {
            String team = selectedTeams.get(p.getUniqueId());
            if (team != null && !team.equalsIgnoreCase("spectator")) {
                fightingTeams.add(team.toLowerCase());
            }
        }

        return fightingTeams.size() >= 2;
    }

    /**
     * 참가자 상태 변경 시 카운트다운 시작 또는 취소 검사
     */
    public void checkCountdownStatus() {
        if (matchInProgress) return;

        if (canStartGame()) {
            if (countdownTask == null) {
                startCountdown();
            }
        } else {
            if (countdownTask != null) {
                cancelCountdown("§c[콜로세움] 참가자의 준비 상태가 해제되어 카운트다운이 취소되었습니다.");
            }
        }
    }

    /**
     * 관리자 전용 강제 시작 (팀 2개 충족 여부와 무관하게 2팀인 척 속이고 3초 카운트다운 후 경기 시작)
     */
    public boolean forceStartGame(Player admin) {
        if (matchInProgress) {
            return false;
        }

        // 대기실에 아무도 없는데 관리자가 실행한 경우 관리자를 참가자로 등록
        if (admin != null && insidePlayers.isEmpty()) {
            insidePlayers.add(admin.getUniqueId());
        }

        List<Player> participants = getInsideOnlinePlayers();
        if (participants.isEmpty()) {
            return false;
        }

        // 직업 미선택자에게 기본 직업(검투사) 부여, 팀 미선택자에게 기본 red 부여
        for (Player p : participants) {
            selectedClasses.putIfAbsent(p.getUniqueId(), "검투사");
            selectedTeams.putIfAbsent(p.getUniqueId(), "red");
            readyPlayers.add(p.getUniqueId());
        }

        // 활성 팀 검사 (1개뿐이면 가상 팀 하나를 추가하여 2개 팀인 척 속임)
        Set<String> fightingTeams = new HashSet<>();
        for (Player p : participants) {
            String team = selectedTeams.get(p.getUniqueId());
            if (team != null && !team.equalsIgnoreCase("spectator")) {
                fightingTeams.add(formatTeamName(team));
            }
        }
        if (fightingTeams.size() < 2) {
            String virtualTeam = fightingTeams.contains("§cRed팀") ? "§9Blue팀" : "§cRed팀";
            fightingTeams.add(virtualTeam + " §7(가상)");
        }

        String teamListStr = String.join(" §7vs ", fightingTeams);
        broadcastToTownWorld("§6[콜로세움] §e관리자가 결투를 시작합니다! §f[" + teamListStr + "§f]");

        if (countdownTask != null) {
            countdownTask.cancel();
        }
        countdownSeconds = 3;
        broadcastCountdownTick(3);

        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            countdownSeconds--;
            if (countdownSeconds > 0) {
                broadcastCountdownTick(countdownSeconds);
            } else {
                if (countdownTask != null) {
                    countdownTask.cancel();
                    countdownTask = null;
                }
                transitionToArena();
            }
        }, 20L, 20L);

        return true;
    }

    /**
     * 3초 카운트다운 시작 (3 -> 2 -> 1 -> 아레나 이동 전환)
     */
    private void startCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
        }
        countdownSeconds = 3;

        // 즉시 3초 알림 전송
        broadcastCountdownTick(3);

        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            countdownSeconds--;
            if (!canStartGame()) {
                cancelCountdown("§c[콜로세움] 참가자의 준비 상태가 변경되어 시작 카운트다운이 취소되었습니다.");
                return;
            }

            if (countdownSeconds > 0) {
                broadcastCountdownTick(countdownSeconds);
            } else {
                if (countdownTask != null) {
                    countdownTask.cancel();
                    countdownTask = null;
                }
                transitionToArena();
            }
        }, 20L, 20L);
    }

    /**
     * 3초 카운트다운 완료 후:
     * "아레나로 이동합니다" (1초 대기) + (어둠 + 실명 + 구속) + 인벤토리 백업 & 클리어
     * 이동 후에는 어둠/실명을 강제 clear하지 않고 지속시간(2.5초) 만료로 자연스럽게 깜빡이며 풀리게 연출!
     * 이동 직후 2초간 "이동 중입니다..." 타이틀 표시 -> 5초 룰렛 -> 2초 맵 확정 -> FIGHT!
     */
    public void transitionToArena() {
        if (arenaWorld == null) {
            initArenaWorld();
        }
        if (arenaWorld == null) {
            broadcastToTownWorld("§c[콜로세움] 결투장 월드를 불러오지 못해 게임을 시작할 수 없습니다.");
            return;
        }

        matchInProgress = true;
        List<Player> participants = getInsideOnlinePlayers();

        for (Player p : participants) {
            p.sendTitle("§6§l아레나로 이동합니다", "§7잠시 후 결투장으로 이동합니다...", 0, 25, 5);
            try {
                p.playSound(p.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.8f, 1.2f);
            } catch (Exception ignored) {}

            // 실명 + 어둠 지속시간: 50틱 (2.5초) -> 이동 후 "이동 중입니다..." 동안 자연스럽게 깜빡거리며 만료됨!
            p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 50, 0, false, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 50, 0, false, false, false));
            // 구속은 FIGHT 시작 전까지 넉넉하게 유지 (60초)
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 1200, 255, false, false, false));

            // 인벤토리 백업 후 클리어
            backupPlayer(p);
            p.getInventory().clear();
        }

        // 1초(20틱) 대기 후 아레나로 텔레포트
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // 참가 플레이어가 있는 활성 팀(색상)들을 순서대로 추출 (관전자 제외)
            List<String> colorPriority = List.of("red", "blue", "green", "yellow");
            List<String> activeTeams = new ArrayList<>();
            for (Player p : participants) {
                if (!p.isOnline()) continue;
                String team = selectedTeams.get(p.getUniqueId());
                if (team != null && !team.equalsIgnoreCase("spectator")) {
                    String lower = team.toLowerCase();
                    if (!activeTeams.contains(lower)) {
                        activeTeams.add(lower);
                    }
                }
            }
            // 일관된 순서로 정렬
            activeTeams.sort(Comparator.comparingInt(t -> {
                int idx = colorPriority.indexOf(t);
                return idx >= 0 ? idx : 999;
            }));

            // 색상과 상관없이 참가 팀 수에 따라 1번 팀은 team1, 2번 팀은 team2, 3번 팀은 team3 스폰으로 순차 배정!
            // 예: 노파 있어도 team1 team2, 빨파 있어도 team1 team2, 파초 있어도 team1 team2
            Map<String, String> teamToSpawnKey = new HashMap<>();
            for (int i = 0; i < activeTeams.size(); i++) {
                teamToSpawnKey.put(activeTeams.get(i), "team" + (i + 1));
            }

            for (Player p : participants) {
                if (!p.isOnline()) continue;

                String team = selectedTeams.get(p.getUniqueId());
                Location targetLoc = null;

                if (team != null && team.equalsIgnoreCase("spectator")) {
                    if (templateManager != null) {
                        targetLoc = templateManager.getTeamSpawn("spectator");
                    }
                } else if (team != null) {
                    String spawnSlot = teamToSpawnKey.get(team.toLowerCase());
                    if (spawnSlot != null && templateManager != null) {
                        targetLoc = templateManager.getTeamSpawn(spawnSlot);
                    }
                }

                if (targetLoc == null || targetLoc.getWorld() == null) {
                    targetLoc = getArenaSpawnLocation();
                }
                p.teleport(targetLoc);

                // 어둠/실명을 강제로 clear하지 않고, 자연스럽게 깜빡거리며 풀리도록 둠
                // "이동 중입니다..." 타이틀을 2초(40틱) 동안 출력
                p.sendTitle("§f§l이동 중입니다...", "§7결투장에 입장하고 있습니다", 0, 40, 10);
            }

            // 2초(40틱) 동안 이동 중 타이틀과 깜빡이며 풀리는 시야를 보여준 후, 5초 룰렛 시작!
            Bukkit.getScheduler().runTaskLater(plugin, () -> startMapRoulette(participants), 40L);
        }, 20L);
    }

    /**
     * 맵 선택 룰렛 연출 (약 5초 동안 점점 느려지는 박자감):
     * 타이틀: "맵을 고르는 중입니다..."
     * 섭타이틀: 경기장 이름들이 룰렛처럼 돌아가며 점차 느려짐 (띠리리리... 띡.. 띡... 띵!)
     * 띵! 하면 맵 로드 + 키트 지급 -> 2초간 맵 확정 타이틀 보여주기 -> 3..2..1.. FIGHT 카운트다운!
     */
    private void startMapRoulette(List<Player> participants) {
        Set<String> registered = (templateManager != null) ? templateManager.getRegisteredMaps() : Collections.emptySet();
        List<String> mapPool = new ArrayList<>(registered);
        if (mapPool.isEmpty()) {
            mapPool.add("기본 경기장");
        }

        // 약 5초(약 104틱) 동안 점차 감속되는 21단계 딜레이 시퀀스
        int[] stepDelays = { 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 5, 5, 6, 7, 9, 12, 16 };
        Random random = new Random();
        String chosenMap = mapPool.get(random.nextInt(mapPool.size()));

        runRouletteStep(participants, mapPool, chosenMap, stepDelays, 0);
    }

    private void runRouletteStep(List<Player> participants, List<String> mapPool, String chosenMap, int[] stepDelays, int stepIndex) {
        if (!matchInProgress) return;

        if (stepIndex < stepDelays.length) {
            String currentDisplayMap = mapPool.get(stepIndex % mapPool.size());
            float pitch = Math.max(0.8f, 2.0f - (stepIndex * 0.055f));

            for (Player p : participants) {
                if (!p.isOnline()) continue;
                p.sendTitle("§e§l맵을 고르는 중입니다...", "§f§l[ §b" + currentDisplayMap + " §f§l]", 0, 15, 0);
                try {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, pitch);
                } catch (Exception ignored) {}
            }

            int delay = stepDelays[stepIndex];
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    runRouletteStep(participants, mapPool, chosenMap, stepDelays, stepIndex + 1), delay);
        } else {
            // 마지막: 띵! (최종 당첨 맵 확정) - 2초(40틱) 동안 큼직하게 노출
            for (Player p : participants) {
                if (!p.isOnline()) continue;
                p.sendTitle("§a§l맵 확정!", "§6§l★ " + chosenMap + " ★", 5, 40, 10);
                try {
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                } catch (Exception ignored) {}
            }

            // 맵 붙여넣기 로드
            if (templateManager != null && !chosenMap.equalsIgnoreCase("기본 경기장")) {
                templateManager.pasteTemplate(chosenMap, true);
            }

            // 직업 키트 100% 확실 지급
            for (Player p : participants) {
                if (!p.isOnline()) continue;
                String team = selectedTeams.get(p.getUniqueId());
                boolean isSpectator = "spectator".equalsIgnoreCase(team);

                if (isSpectator) {
                    p.setGameMode(GameMode.SPECTATOR);
                    p.sendMessage("§7[콜로세움] 관전자로 경기장에 입장하였습니다.");
                } else {
                    p.setGameMode(GameMode.SURVIVAL);
                    try {
                        p.setHealth(p.getMaxHealth());
                    } catch (Exception ignored) {}
                    p.setFoodLevel(20);
                    p.setSaturation(20.0f);

                    // 선택 직업 가져오기 (없으면 기본 검투사)
                    String className = selectedClasses.get(p.getUniqueId());
                    if (className == null || className.isBlank()) {
                        className = "검투사";
                        selectedClasses.put(p.getUniqueId(), className);
                    }

                    ColosseumClassType classType = ColosseumClassType.fromDisplayName(className);
                    if (classType != null) {
                        classType.applyKit(p);
                        p.sendMessage("§6[콜로세움] §a'" + classType.getDisplayName() + "' 직업 키트가 정상 지급되었습니다!");
                    } else {
                        plugin.getLogger().warning("[콜로세움] 직업 키트 찾기 실패: " + className);
                    }
                }
            }

            // 당첨 맵 이름을 2초(40틱) 동안 충분히 보여준 후, 3... 2... 1... FIGHT 카운트다운 시작!
            Bukkit.getScheduler().runTaskLater(plugin, () -> startArenaFightCountdown(participants), 40L);
        }
    }

    /**
     * 아레나 결투 시작 카운트다운 (3... 2... 1... FIGHT!)
     * FIGHT 출력 시 구속 완전 해제!
     */
    private void startArenaFightCountdown(List<Player> participants) {
        if (!matchInProgress) return;

        int[] count = { 3 };
        org.bukkit.scheduler.BukkitTask[] taskHolder = new org.bukkit.scheduler.BukkitTask[1];

        // 즉시 3... 출력
        broadcastFightCountTick(participants, 3);

        taskHolder[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!matchInProgress) {
                if (taskHolder[0] != null) taskHolder[0].cancel();
                return;
            }

            count[0]--;
            if (count[0] > 0) {
                broadcastFightCountTick(participants, count[0]);
            } else {
                if (taskHolder[0] != null) {
                    taskHolder[0].cancel();
                }

                // FIGHT! 출력 및 구속 해제!
                for (Player p : participants) {
                    if (!p.isOnline()) continue;
                    p.sendTitle("§a§lFIGHT!", "§f결투가 시작되었습니다!", 0, 30, 10);
                    try {
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.2f);
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    } catch (Exception ignored) {}

                    // 구속 디버프 완전 해제 -> 자유 이동 가능!
                    p.removePotionEffect(PotionEffectType.SLOWNESS);
                    p.sendMessage("§6[콜로세움] §aFIGHT! 승리를 위해 싸우세요!");
                }

                broadcastToTownWorld("§6[콜로세움] §a아레나 결투가 시작되었습니다!");
            }
        }, 20L, 20L);
    }

    private void broadcastFightCountTick(List<Player> participants, int sec) {
        String titleColor = switch (sec) {
            case 3 -> "§e§l3...";
            case 2 -> "§6§l2...";
            case 1 -> "§c§l1...";
            default -> "§f§l";
        };
        float pitch = switch (sec) {
            case 3 -> 1.0f;
            case 2 -> 1.25f;
            case 1 -> 1.5f;
            default -> 1.0f;
        };

        for (Player p : participants) {
            if (!p.isOnline()) continue;
            p.sendTitle(titleColor, "§f준비하세요...", 0, 25, 5);
            try {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, pitch);
            } catch (Exception ignored) {}
        }
    }

    private void broadcastCountdownTick(int sec) {
        String titleColor = switch (sec) {
            case 3 -> "§e§l";
            case 2 -> "§6§l";
            case 1 -> "§c§l";
            default -> "§f§l";
        };
        float pitch = switch (sec) {
            case 3 -> 1.0f;
            case 2 -> 1.25f;
            case 1 -> 1.5f;
            default -> 1.0f;
        };

        for (Player p : getInsideOnlinePlayers()) {
            p.sendTitle(titleColor + sec, "§f경기장으로 입장합니다...", 0, 25, 5);
            try {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, pitch);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 카운트다운 취소
     */
    public void cancelCountdown(String reason) {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        countdownSeconds = 3;

        if (reason != null && !reason.isBlank()) {
            for (Player p : getInsideOnlinePlayers()) {
                p.sendTitle("§c§l카운트다운 취소", "§7준비 상태가 해제되었습니다", 0, 30, 10);
                p.sendMessage(reason);
                try {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * 게임 종료 시퀀스:
     * 1. "000이 승리했습니다" (3초 대기)
     * 2. "마을로 이동합니다" (실명 + 어둠 + 구속) (1초 대기)
     * 3. 맵 초기화 -> 인벤 복구 -> 마을 이동
     */
    public void endGame(String winnerMessage) {
        cancelCountdown(null);

        String winnerText = (winnerMessage != null && !winnerMessage.isBlank()) ? winnerMessage : "게임을 종료했습니다";

        // 경기장에 있거나 백업 데이터가 있는 모든 대상 플레이어 수집
        Set<Player> affected = new HashSet<>();
        for (UUID uuid : inventoryBackups.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) affected.add(p);
        }
        for (Player p : getInsideOnlinePlayers()) {
            affected.add(p);
        }
        if (arenaWorld != null) {
            for (Player p : arenaWorld.getPlayers()) {
                affected.add(p);
            }
        }

        // [단계 1] "000이 승리했습니다" 출력 + 3초 대기
        for (Player p : affected) {
            p.sendTitle("§6§l게임 종료", "§e§l" + winnerText, 5, 55, 10);
            try {
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            } catch (Exception ignored) {}
            p.sendMessage("§6[콜로세움] §e" + winnerText);
        }

        // 3초(60틱) 대기
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // [단계 2] "마을로 이동합니다" 타이틀 + (실명 + 어둠 + 구속) 부여 + 1초 대기
            for (Player p : affected) {
                if (!p.isOnline()) continue;
                p.sendTitle("§f§l마을로 이동합니다", "§7잠시만 기다려주세요...", 0, 25, 5);
                try {
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.0f);
                } catch (Exception ignored) {}

                // 실명 + 어둠 + 구속 디버프
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0, false, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0, false, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 255, false, false, false));
            }

            // 1초(20틱) 대기
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // [단계 3] 맵 초기화 -> 인벤 복구 -> 마을 이동 및 콜로세움 퇴장 처리

                // 1. 맵 초기화
                if (templateManager != null) {
                    templateManager.resetToBaseArena();
                }

                // 2. 인벤 복구 (디버프도 백업 상태로 완전 복원)
                restoreAllPlayers();

                // 3. 마을 이동 및 퇴장 처리
                for (Player p : affected) {
                    if (!p.isOnline()) continue;
                    p.setGameMode(GameMode.SURVIVAL);

                    // 잔여 디버프 및 직업 버프 정리
                    p.removePotionEffect(PotionEffectType.BLINDNESS);
                    p.removePotionEffect(PotionEffectType.DARKNESS);
                    p.removePotionEffect(PotionEffectType.SLOWNESS);
                    p.removePotionEffect(PotionEffectType.STRENGTH);

                    // 액션바 비우기
                    try {
                        p.sendActionBar(net.kyori.adventure.text.Component.empty());
                    } catch (Exception ignored) {}

                    try {
                        Hez.Menu.WarpManager.teleport(p, "town");
                    } catch (Exception e) {
                        World townW = Bukkit.getWorld(townWorldName);
                        if (townW != null) {
                            p.teleport(new Location(townW, (minX + maxX) / 2.0 + 0.5, 65.0, (minZ + maxZ) / 2.0 + 0.5));
                        }
                    }
                }

                // 콜로세움 내부 플레이어 목록 및 상태 완전 초기화 (퇴장 처리 완료)
                insidePlayers.clear();
                readyPlayers.clear();
                selectedClasses.clear();
                selectedTeams.clear();
                inventoryBackups.clear();
                playerPos1Selection.clear();
                playerTeamPos1Selection.clear();

                matchInProgress = false;

                broadcastToTownWorld("§6[콜로세움] §e결투가 종료되어 모든 플레이어가 마을로 귀환했습니다.");
                plugin.getLogger().info("[콜로세움] 결투 정상 종료 및 마을 복귀, 콜로세움 완전 퇴장 처리 완료.");
            }, 20L);
        }, 60L);
    }

    /**
     * 플레이어의 현재 인벤토리 및 상태를 백업
     */
    public void backupPlayer(Player player) {
        if (player == null) return;
        inventoryBackups.put(player.getUniqueId(), new PlayerBackupData(player));
    }

    /**
     * 백업된 인벤토리 및 상태를 플레이어에게 복원
     */
    public void restorePlayer(Player player) {
        if (player == null) return;
        PlayerBackupData backup = inventoryBackups.remove(player.getUniqueId());
        if (backup != null) {
            backup.restore(player);
            player.sendMessage("§a[콜로세움] 기존 인벤토리와 상태가 정상 복구되었습니다.");
        }
    }

    /**
     * 모든 백업 대상 플레이어의 인벤토리 복원
     */
    public void restoreAllPlayers() {
        for (Map.Entry<UUID, PlayerBackupData> entry : inventoryBackups.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null && p.isOnline()) {
                entry.getValue().restore(p);
            }
        }
        inventoryBackups.clear();
    }

    /**
     * 특정 팀의 구역 범위를 설정 파일에 저장 (X1, Z1 ~ X2, Z2)
     */
    public void saveTeamArea(String teamName, int x1, int z1, int x2, int z2) {
        teamName = teamName.toLowerCase();
        TeamArea area = new TeamArea(x1, z1, x2, z2);
        teamAreas.put(teamName, area);
        if (config == null) {
            config = YamlConfiguration.loadConfiguration(configFile);
        }
        config.set("teams." + teamName + ".minX", area.getMinX());
        config.set("teams." + teamName + ".maxX", area.getMaxX());
        config.set("teams." + teamName + ".minZ", area.getMinZ());
        config.set("teams." + teamName + ".maxZ", area.getMaxZ());
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("팀 구역 저장 실패: " + e.getMessage());
        }
    }

    public static String formatTeamName(String teamKey) {
        if (teamKey == null) return "미선택";
        return switch (teamKey.toLowerCase()) {
            case "red" -> "§cRed팀";
            case "blue" -> "§9Blue팀";
            case "green" -> "§aGreen팀";
            case "yellow" -> "§eYellow팀";
            case "spectator" -> "§7관전자";
            default -> teamKey;
        };
    }

    /**
     * 특정 직업의 발판 위치를 설정 파일에 저장 (X, Z만 저장)
     */
    public void saveClassPosition(String className, int x, int z) {
        classPositions.put(className, new int[]{x, z});
        if (config == null) {
            config = YamlConfiguration.loadConfiguration(configFile);
        }
        config.set("classes." + className + ".x", x);
        config.set("classes." + className + ".z", z);
        config.set("classes." + className + ".y", null); // 혹시 남아있던 y 제거
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("직업 좌표 저장 실패: " + e.getMessage());
        }
    }

    /**
     * 콜로세움 내부에 있는 플레이어들에게 액션바로 참가자 목록 및 본인 선택 직업/팀 지속 전송
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
            String myTeam = selectedTeams.get(p.getUniqueId());
            boolean isReady = readyPlayers.contains(p.getUniqueId());
            String classInfo = (myClass != null) ? " §7| §b직업: §f" + myClass : " §7| §7(직업 미선택)";
            String teamInfo = (myTeam != null) ? " §7| §6팀: §f" + formatTeamName(myTeam) : " §7| §7(팀 미선택)";
            String readyInfo = isReady ? " §a[준비완료 ✔]" : " §7[대기중 …]";
            net.kyori.adventure.text.Component actionBarComponent =
                    net.kyori.adventure.text.Component.text("§6[콜로세움] §e참가자 : §f" + playerNames + classInfo + teamInfo + " " + readyInfo);
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
        cancelCountdown(null);
        matchInProgress = false;
        restoreAllPlayers();
        if (actionBarTask != null) {
            actionBarTask.cancel();
            actionBarTask = null;
        }
        for (Player p : getInsideOnlinePlayers()) {
            try {
                p.sendActionBar(net.kyori.adventure.text.Component.empty());
                p.removePotionEffect(PotionEffectType.STRENGTH);
            } catch (Exception ignored) {
            }
        }
        insidePlayers.clear();
        playerPos1Selection.clear();
        selectedClasses.clear();
        selectedTeams.clear();
        readyPlayers.clear();
        playerTeamPos1Selection.clear();
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

    public void setPlayerTeamPos1(UUID uuid, int x, int z) {
        playerTeamPos1Selection.put(uuid, new int[]{x, z});
    }

    public int[] getPlayerTeamPos1(UUID uuid) {
        return playerTeamPos1Selection.get(uuid);
    }

    public void clearPlayerTeamPos1(UUID uuid) {
        playerTeamPos1Selection.remove(uuid);
    }

    public String getSelectedClass(UUID uuid) {
        return selectedClasses.get(uuid);
    }

    public String getSelectedTeam(UUID uuid) {
        return selectedTeams.get(uuid);
    }

    public Map<String, int[]> getClassPositions() {
        return Collections.unmodifiableMap(classPositions);
    }

    public Map<String, TeamArea> getTeamAreas() {
        return Collections.unmodifiableMap(teamAreas);
    }

    // Getters & Setters
    public int getMinX() { return minX; }
    public int getMaxX() { return maxX; }
    public int getMinZ() { return minZ; }
    public int getMaxZ() { return maxZ; }
    public String getTownWorldName() { return townWorldName; }
    public Set<UUID> getInsidePlayers() { return insidePlayers; }
    public Map<UUID, String> getSelectedClasses() { return selectedClasses; }
    public Map<UUID, String> getSelectedTeams() { return selectedTeams; }
    public Set<UUID> getReadyPlayers() { return Collections.unmodifiableSet(readyPlayers); }
    public boolean isReady(UUID uuid) { return readyPlayers.contains(uuid); }
    public boolean isMatchInProgress() { return matchInProgress; }
    public void setMatchInProgress(boolean inProgress) { this.matchInProgress = inProgress; }
    public ArenaTemplateManager getTemplateManager() { return templateManager; }
}
