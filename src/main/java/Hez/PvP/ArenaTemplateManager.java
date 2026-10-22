package Hez.PvP;

import Hez.MineFarm.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ArenaTemplateManager {

    private final Main plugin;
    private final ColosseumManager colosseumManager;
    private final File templateFolder;
    private final File configFile;
    private FileConfiguration config;

    // 경기장 고정 붙여넣기 기준 좌표 (최소 X, Y, Z가 위치할 곳)
    private Location pasteLocation;

    // 아레나 전투 영역 경계 (이 범위 밖은 프리셋 보관 구역이므로 절대 건드리지 않음)
    private boolean hasBounds = false;
    private int boundsMinX, boundsMaxX;
    private int boundsMinY, boundsMaxY;
    private int boundsMinZ, boundsMaxZ;

    // 팀별 소환 좌표 (team1, team2, team3, team4, spectator)
    private final Map<String, Location> teamSpawns = new ConcurrentHashMap<>();

    // 현재 테마 경기장 때문에 배치된 블록 좌표 목록 (초기화 시 공기화 대상)
    private final Set<Location> currentThemeBlocks = ConcurrentHashMap.newKeySet();

    // 게임 진행 중 플레이어가 설치한 블록 좌표 목록 (초기화 시 공기화 대상)
    private final Set<Location> playerPlacedBlocks = ConcurrentHashMap.newKeySet();

    // 게임 진행 중 플레이어가 파괴한 블록 좌표 및 원래 블록 데이터 (초기화 시 원상복구 대상)
    private final Map<Location, BlockData> playerBrokenBlocks = new ConcurrentHashMap<>();

    // 등록된 경기장 맵 이름 목록
    private final Set<String> registeredMaps = Collections.synchronizedSet(new LinkedHashSet<>());

    public static class TemplateBlock {
        public final int dx;
        public final int dy;
        public final int dz;
        public final String blockDataString;

        public TemplateBlock(int dx, int dy, int dz, String blockDataString) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            this.blockDataString = blockDataString;
        }
    }

    public static class ArenaTemplate {
        public final String name;
        public final int sizeX;
        public final int sizeY;
        public final int sizeZ;
        public final boolean isVoidMode;
        public final List<TemplateBlock> blocks;

        public ArenaTemplate(String name, int sizeX, int sizeY, int sizeZ, boolean isVoidMode, List<TemplateBlock> blocks) {
            this.name = name;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.isVoidMode = isVoidMode;
            this.blocks = blocks;
        }

        public ArenaTemplate(String name, int sizeX, int sizeY, int sizeZ, List<TemplateBlock> blocks) {
            this(name, sizeX, sizeY, sizeZ, false, blocks);
        }
    }

    public ArenaTemplateManager(Main plugin, ColosseumManager colosseumManager) {
        this.plugin = plugin;
        this.colosseumManager = colosseumManager;
        this.templateFolder = new File(plugin.getDataFolder(), "arena_templates");
        this.configFile = new File(plugin.getDataFolder(), "arenaSettings.yml");
    }

    public void init() {
        if (!templateFolder.exists()) {
            templateFolder.mkdirs();
        }
        loadConfig();
    }

    public void loadConfig() {
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException ignored) {}
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        World arenaWorld = colosseumManager.getArenaWorld();
        if (arenaWorld == null) {
            arenaWorld = Bukkit.getWorld(colosseumManager.getArenaWorldName());
        }

        // 1. 고정 붙여넣기 좌표 로드
        if (config.contains("pasteLocation.x") && arenaWorld != null) {
            double px = config.getDouble("pasteLocation.x");
            double py = config.getDouble("pasteLocation.y");
            double pz = config.getDouble("pasteLocation.z");
            this.pasteLocation = new Location(arenaWorld, px, py, pz);
        } else if (arenaWorld != null) {
            this.pasteLocation = new Location(arenaWorld, 0, 4, 0);
        }

        // 2. 팀별 스폰 좌표 로드
        teamSpawns.clear();
        ConfigurationSection spawnSec = config.getConfigurationSection("spawns");
        if (spawnSec != null && arenaWorld != null) {
            for (String key : spawnSec.getKeys(false)) {
                double sx = spawnSec.getDouble(key + ".x");
                double sy = spawnSec.getDouble(key + ".y");
                double sz = spawnSec.getDouble(key + ".z");
                float yaw = (float) spawnSec.getDouble(key + ".yaw", 0.0);
                float pitch = (float) spawnSec.getDouble(key + ".pitch", 0.0);
                teamSpawns.put(key.toLowerCase(), new Location(arenaWorld, sx, sy, sz, yaw, pitch));
            }
        }

        // 3. 아레나 전투 영역 경계 로드
        if (config.contains("bounds.minX")) {
            this.boundsMinX = config.getInt("bounds.minX");
            this.boundsMaxX = config.getInt("bounds.maxX");
            this.boundsMinY = config.getInt("bounds.minY");
            this.boundsMaxY = config.getInt("bounds.maxY");
            this.boundsMinZ = config.getInt("bounds.minZ");
            this.boundsMaxZ = config.getInt("bounds.maxZ");
            this.hasBounds = true;
        }

        // 4. 등록된 맵 목록 갱신 (폴더 내 .template 파일 기준)
        refreshRegisteredMaps();
    }

    public void saveConfig() {
        if (config == null) {
            config = YamlConfiguration.loadConfiguration(configFile);
        }
        if (pasteLocation != null) {
            config.set("pasteLocation.x", pasteLocation.getX());
            config.set("pasteLocation.y", pasteLocation.getY());
            config.set("pasteLocation.z", pasteLocation.getZ());
        }
        if (hasBounds) {
            config.set("bounds.minX", boundsMinX);
            config.set("bounds.maxX", boundsMaxX);
            config.set("bounds.minY", boundsMinY);
            config.set("bounds.maxY", boundsMaxY);
            config.set("bounds.minZ", boundsMinZ);
            config.set("bounds.maxZ", boundsMaxZ);
        }
        for (Map.Entry<String, Location> entry : teamSpawns.entrySet()) {
            Location loc = entry.getValue();
            String key = entry.getKey();
            config.set("spawns." + key + ".x", loc.getX());
            config.set("spawns." + key + ".y", loc.getY());
            config.set("spawns." + key + ".z", loc.getZ());
            config.set("spawns." + key + ".yaw", loc.getYaw());
            config.set("spawns." + key + ".pitch", loc.getPitch());
        }
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("arenaSettings.yml 저장 실패: " + e.getMessage());
        }
    }

    public void refreshRegisteredMaps() {
        registeredMaps.clear();
        if (!templateFolder.exists()) return;
        File[] files = templateFolder.listFiles((dir, name) -> name.endsWith(".template") && !name.equalsIgnoreCase("main.template"));
        if (files != null) {
            for (File file : files) {
                String mapName = file.getName().substring(0, file.getName().length() - ".template".length());
                registeredMaps.add(mapName);
            }
        }
    }

    /**
     * 특정 3D 영역을 스캔하여 템플릿 파일로 저장 (main 또는 테마 맵)
     */
    public boolean saveTemplate(String templateName, Location loc1, Location loc2) {
        return saveTemplate(templateName, loc1, loc2, false);
    }

    public boolean saveTemplate(String templateName, Location loc1, Location loc2, boolean isVoidMode) {
        if (loc1 == null || loc2 == null || loc1.getWorld() == null) return false;
        World world = loc1.getWorld();

        int minX = Math.min(loc1.getBlockX(), loc2.getBlockX());
        int maxX = Math.max(loc1.getBlockX(), loc2.getBlockX());
        int minY = Math.min(loc1.getBlockY(), loc2.getBlockY());
        int maxY = Math.max(loc1.getBlockY(), loc2.getBlockY());
        int minZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        int maxZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ());

        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;

        List<TemplateBlock> blockList = new ArrayList<>();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    org.bukkit.block.Block block = world.getBlockAt(x, y, z);
                    // 빈 공기 블록은 저장 시 제외하여 파일 용량 대폭 절감 및 Air 무시 최적화
                    if (block.getType().isAir()) {
                        continue;
                    }
                    int dx = x - minX;
                    int dy = y - minY;
                    int dz = z - minZ;
                    blockList.add(new TemplateBlock(dx, dy, dz, block.getBlockData().getAsString()));
                }
            }
        }

        File file = new File(templateFolder, templateName.toLowerCase() + ".template");
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(file))))) {
            dos.writeInt(2); // 파일 버전 2 (isVoidMode 포함)
            dos.writeBoolean(isVoidMode);
            dos.writeInt(sizeX);
            dos.writeInt(sizeY);
            dos.writeInt(sizeZ);
            dos.writeInt(blockList.size());

            for (TemplateBlock tb : blockList) {
                dos.writeInt(tb.dx);
                dos.writeInt(tb.dy);
                dos.writeInt(tb.dz);
                dos.writeUTF(tb.blockDataString);
            }
            dos.flush();
        } catch (IOException e) {
            plugin.getLogger().severe("경기장 템플릿 저장 실패 (" + templateName + "): " + e.getMessage());
            return false;
        }

        if (!templateName.equalsIgnoreCase("main")) {
            registeredMaps.add(templateName);
        }
        return true;
    }

    /**
     * 템플릿 파일 로드
     */
    public ArenaTemplate loadTemplate(String templateName) {
        File file = new File(templateFolder, templateName.toLowerCase() + ".template");
        if (!file.exists()) return null;

        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(file))))) {
            int version = dis.readInt();
            boolean isVoidMode = false;
            if (version >= 2) {
                isVoidMode = dis.readBoolean();
            }
            int sizeX = dis.readInt();
            int sizeY = dis.readInt();
            int sizeZ = dis.readInt();
            int count = dis.readInt();

            List<TemplateBlock> blocks = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                int dx = dis.readInt();
                int dy = dis.readInt();
                int dz = dis.readInt();
                String dataStr = dis.readUTF();
                blocks.add(new TemplateBlock(dx, dy, dz, dataStr));
            }
            return new ArenaTemplate(templateName, sizeX, sizeY, sizeZ, isVoidMode, blocks);
        } catch (IOException e) {
            plugin.getLogger().severe("경기장 템플릿 로드 실패 (" + templateName + "): " + e.getMessage());
            return null;
        }
    }

    /**
     * 공허(Void) 맵을 위해 기본 경기장(main)의 바닥 블록들을 공기(Air)로 비움
     * 외벽과 관중석은 건드리지 않고 기본 바닥 블록들만 지워짐
     */
    public void clearBaseFloor() {
        if (pasteLocation == null || pasteLocation.getWorld() == null) return;
        ArenaTemplate mainTpl = loadTemplate("main");
        if (mainTpl == null) return;

        World world = pasteLocation.getWorld();
        int originX = pasteLocation.getBlockX();
        int originY = pasteLocation.getBlockY();
        int originZ = pasteLocation.getBlockZ();

        for (TemplateBlock tb : mainTpl.blocks) {
            Location loc = new Location(world, originX + tb.dx, originY + tb.dy, originZ + tb.dz);
            if (isInArenaBounds(loc)) {
                loc.getBlock().setType(Material.AIR, false);
                currentThemeBlocks.add(loc);
            }
        }
    }

    /**
     * 경기장 템플릿을 고정 좌표에 붙여넣기 (Air 무시 지원)
     */
    public boolean pasteTemplate(String templateName, boolean isThemeMap) {
        if (pasteLocation == null || pasteLocation.getWorld() == null) {
            return false;
        }
        ArenaTemplate template = loadTemplate(templateName);
        if (template == null) {
            return false;
        }

        // 공허 모드 테마 맵인 경우 기본 바닥을 먼저 공기화하여 허공 낭떠러지 생성!
        if (isThemeMap && template.isVoidMode) {
            clearBaseFloor();
        }

        World world = pasteLocation.getWorld();
        int originX = pasteLocation.getBlockX();
        int originY = pasteLocation.getBlockY();
        int originZ = pasteLocation.getBlockZ();

        for (TemplateBlock tb : template.blocks) {
            try {
                BlockData blockData = Bukkit.createBlockData(tb.blockDataString);
                Location targetLoc = new Location(world, originX + tb.dx, originY + tb.dy, originZ + tb.dz);

                // 아레나 경계(Bounds)가 설정되어 있다면 내부만 적용 (경기장 외벽/관중석 프리셋 완벽 보호)
                if (hasBounds() && !isInArenaBounds(targetLoc)) {
                    continue;
                }

                // 테마 맵일 때만 Air를 무시하여 기존 타원형 아레나 외벽 형태 보존
                // 기본 경기장(main) 리셋일 때는 Air를 무시하지 않고 공기 블록까지 그대로 덮어써서 완전 초기화!
                if (isThemeMap && blockData.getMaterial().isAir()) {
                    continue;
                }

                world.getBlockAt(targetLoc).setBlockData(blockData, false);

                if (isThemeMap) {
                    currentThemeBlocks.add(targetLoc);
                }
            } catch (Exception ignored) {}
        }
        return true;
    }

    /**
     * 기본 경기장으로 완전 원상복구 리셋
     * 1. 게임 중 플레이어가 설치한 블록들 공기화 (아레나 내부만)
     * 2. 이전 테마 경기장 블록들 공기화 (아레나 내부만)
     * 3. 게임 중 플레이어가 파괴했던 블록들을 원래 상태로 완벽 복구!
     * 4. 기본 경기장(main) 템플릿을 Air 무시로 재부착
     * (아레나 경계 바깥의 프리셋 건축물은 절대 건드리지 않음)
     */
    public boolean resetToBaseArena() {
        if (pasteLocation == null || pasteLocation.getWorld() == null) {
            return false;
        }

        // 1. 플레이어 설치 블록 제거
        for (Location loc : playerPlacedBlocks) {
            if (loc.getWorld() != null && isInArenaBounds(loc)) {
                loc.getBlock().setType(Material.AIR, false);
            }
        }
        playerPlacedBlocks.clear();

        // 2. 테마 경기장 잔여 블록 제거
        for (Location loc : currentThemeBlocks) {
            if (loc.getWorld() != null && isInArenaBounds(loc)) {
                loc.getBlock().setType(Material.AIR, false);
            }
        }
        currentThemeBlocks.clear();

        // 3. 플레이어가 파괴했던 블록들 원래대로 복구 (구멍 메우기)
        for (Map.Entry<Location, BlockData> entry : playerBrokenBlocks.entrySet()) {
            Location loc = entry.getKey();
            if (loc.getWorld() != null && isInArenaBounds(loc)) {
                loc.getBlock().setBlockData(entry.getValue(), false);
            }
        }
        playerBrokenBlocks.clear();

        // 4. 메인 기본 경기장 붙여넣기 (Air 포함하여 완전 초기화)
        File mainFile = new File(templateFolder, "main.template");
        if (mainFile.exists()) {
            return pasteTemplate("main", false);
        }
        return true;
    }

    public boolean deleteTemplate(String templateName) {
        File file = new File(templateFolder, templateName.toLowerCase() + ".template");
        if (file.exists() && file.delete()) {
            registeredMaps.remove(templateName);
            return true;
        }
        return false;
    }

    /**
     * 특정 위치가 아레나 전투 영역(Bounds) 내부인지 검사
     */
    public boolean isInArenaBounds(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        if (!colosseumManager.isArenaWorld(loc.getWorld())) return false;
        if (!hasBounds) return true; // 아직 bounds 미설정 시 월드 내부 전체 대상
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        return x >= boundsMinX && x <= boundsMaxX &&
               y >= boundsMinY && y <= boundsMaxY &&
               z >= boundsMinZ && z <= boundsMaxZ;
    }

    public void setBounds(int x1, int y1, int z1, int x2, int y2, int z2) {
        this.boundsMinX = Math.min(x1, x2);
        this.boundsMaxX = Math.max(x1, x2);
        this.boundsMinY = Math.min(y1, y2);
        this.boundsMaxY = Math.max(y1, y2);
        this.boundsMinZ = Math.min(z1, z2);
        this.boundsMaxZ = Math.max(z1, z2);
        this.hasBounds = true;
        saveConfig();
    }

    public void recordPlayerBlockPlace(Location loc) {
        if (loc == null || !isInArenaBounds(loc)) return;
        playerPlacedBlocks.add(loc.getBlock().getLocation());
    }

    public void recordPlayerBlockBreak(Location loc, BlockData originalData) {
        if (loc == null || originalData == null || !isInArenaBounds(loc)) return;
        Location blockLoc = loc.getBlock().getLocation();
        // 플레이어가 자신이 방금 설치했던 블록을 부순 것이라면 롤백 목록에서 제거
        if (playerPlacedBlocks.remove(blockLoc)) {
            return;
        }
        // 원래 지형/경기장에 있던 블록을 부순 경우 최초 원본 블록 데이터를 보존
        playerBrokenBlocks.putIfAbsent(blockLoc, originalData);
    }

    // Getters & Setters
    public Location getPasteLocation() {
        return pasteLocation;
    }

    public void setPasteLocation(Location pasteLocation) {
        this.pasteLocation = pasteLocation.clone();
        saveConfig();
    }

    public boolean hasBounds() {
        return hasBounds;
    }

    public int getBoundsMinX() { return boundsMinX; }
    public int getBoundsMaxX() { return boundsMaxX; }
    public int getBoundsMinY() { return boundsMinY; }
    public int getBoundsMaxY() { return boundsMaxY; }
    public int getBoundsMinZ() { return boundsMinZ; }
    public int getBoundsMaxZ() { return boundsMaxZ; }

    public void setTeamSpawn(String teamKey, Location loc) {
        if (loc == null) return;
        teamSpawns.put(teamKey.toLowerCase(), loc.clone());
        saveConfig();
    }

    public Location getTeamSpawn(String teamKey) {
        return teamSpawns.get(teamKey.toLowerCase());
    }

    public Map<String, Location> getTeamSpawns() {
        return Collections.unmodifiableMap(teamSpawns);
    }

    public Set<String> getRegisteredMaps() {
        return Collections.unmodifiableSet(registeredMaps);
    }

    public boolean hasMainTemplate() {
        return new File(templateFolder, "main.template").exists();
    }
}
