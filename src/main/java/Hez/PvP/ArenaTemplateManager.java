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

    // 팀별 소환 좌표 (team1, team2, team3, team4, spectator)
    private final Map<String, Location> teamSpawns = new ConcurrentHashMap<>();

    // 현재 테마 경기장 때문에 배치된 블록 좌표 목록 (초기화 시 공기화 대상)
    private final Set<Location> currentThemeBlocks = ConcurrentHashMap.newKeySet();

    // 게임 진행 중 플레이어가 설치한 블록 좌표 목록 (초기화 시 공기화 대상)
    private final Set<Location> playerPlacedBlocks = ConcurrentHashMap.newKeySet();

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
        public final List<TemplateBlock> blocks;

        public ArenaTemplate(String name, int sizeX, int sizeY, int sizeZ, List<TemplateBlock> blocks) {
            this.name = name;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.blocks = blocks;
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

        // 3. 등록된 맵 목록 갱신 (폴더 내 .template 파일 기준)
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
            dos.writeInt(1); // 파일 버전
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
            return new ArenaTemplate(templateName, sizeX, sizeY, sizeZ, blocks);
        } catch (IOException e) {
            plugin.getLogger().severe("경기장 템플릿 로드 실패 (" + templateName + "): " + e.getMessage());
            return null;
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

        World world = pasteLocation.getWorld();
        int originX = pasteLocation.getBlockX();
        int originY = pasteLocation.getBlockY();
        int originZ = pasteLocation.getBlockZ();

        for (TemplateBlock tb : template.blocks) {
            try {
                BlockData blockData = Bukkit.createBlockData(tb.blockDataString);
                // 공기 블록은 무조건 건너뜀 (아레나 관중석/외벽 1도 손상 안 됨)
                if (blockData.getMaterial().isAir()) {
                    continue;
                }

                Location targetLoc = new Location(world, originX + tb.dx, originY + tb.dy, originZ + tb.dz);
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
     * 1. 이전 테마 경기장 블록들 공기화
     * 2. 게임 중 플레이어가 설치한 블록들 공기화
     * 3. 기본 경기장(main) 템플릿을 Air 무시로 재부착
     */
    public boolean resetToBaseArena() {
        if (pasteLocation == null || pasteLocation.getWorld() == null) {
            return false;
        }

        // 1. 테마 경기장 잔여 블록 제거
        for (Location loc : currentThemeBlocks) {
            if (loc.getWorld() != null) {
                loc.getBlock().setType(Material.AIR, false);
            }
        }
        currentThemeBlocks.clear();

        // 2. 플레이어 설치 블록 제거
        for (Location loc : playerPlacedBlocks) {
            if (loc.getWorld() != null) {
                loc.getBlock().setType(Material.AIR, false);
            }
        }
        playerPlacedBlocks.clear();

        // 3. 메인 기본 경기장 붙여넣기 (Air 무시)
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

    public void recordPlayerBlockPlace(Location loc) {
        if (loc != null && colosseumManager.isArenaWorld(loc.getWorld())) {
            playerPlacedBlocks.add(loc);
        }
    }

    // Getters & Setters
    public Location getPasteLocation() {
        return pasteLocation;
    }

    public void setPasteLocation(Location pasteLocation) {
        this.pasteLocation = pasteLocation.clone();
        saveConfig();
    }

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
