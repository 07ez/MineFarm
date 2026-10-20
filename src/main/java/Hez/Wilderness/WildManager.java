package Hez.Wilderness;

import Hez.MineFarm.Main;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public class WildManager {
    public static final String WILD_WORLD_NAME = "wild";

    private final Main plugin;
    private World wildWorld;
    private File wildConfigFile;
    private FileConfiguration wildConfig;
    private Location spawnLocation;
    private WildItemCleaner itemCleaner;

    public WildManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * 야생 월드 생성/로드 및 설정 초기화
     */
    public void init() {
        // 1. 설정 파일 로드
        loadConfig();

        // 2. 야생 월드 생성 및 로드
        WorldCreator creator = new WorldCreator(WILD_WORLD_NAME);
        creator.environment(World.Environment.NORMAL);
        this.wildWorld = Bukkit.createWorld(creator);

        if (this.wildWorld != null) {
            // 야생 월드 기본 게임 규칙 설정
            this.wildWorld.setGameRule(GameRule.DO_MOB_SPAWNING, true);
            this.wildWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
            this.wildWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, true);
            this.wildWorld.setDifficulty(Difficulty.NORMAL);

            // 고정 스폰 위치 로드
            loadSpawnLocation();
            plugin.getLogger().info("야생 월드('" + WILD_WORLD_NAME + "')가 성공적으로 로드되었습니다.");
        } else {
            plugin.getLogger().severe("야생 월드('" + WILD_WORLD_NAME + "')를 생성하거나 불러오지 못했습니다!");
        }

        // 3. 5분 바닥 아이템 청소 태스크 시작
        this.itemCleaner = new WildItemCleaner(plugin, this);
        this.itemCleaner.start();
    }

    private void loadConfig() {
        wildConfigFile = new File(plugin.getDataFolder(), "wildConfig.yml");
        if (!wildConfigFile.exists()) {
            try {
                wildConfigFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("wildConfig.yml 파일 생성 실패: " + e.getMessage());
            }
        }
        wildConfig = YamlConfiguration.loadConfiguration(wildConfigFile);
    }

    private void loadSpawnLocation() {
        if (wildConfig.contains("spawn.x")) {
            double x = wildConfig.getDouble("spawn.x");
            double y = wildConfig.getDouble("spawn.y");
            double z = wildConfig.getDouble("spawn.z");
            float yaw = (float) wildConfig.getDouble("spawn.yaw", 0.0);
            float pitch = (float) wildConfig.getDouble("spawn.pitch", 0.0);
            this.spawnLocation = new Location(wildWorld, x, y, z, yaw, pitch);
        } else {
            // 설정된 스폰이 없으면 월드의 기본 스폰 위치 사용
            this.spawnLocation = wildWorld.getSpawnLocation().clone().add(0.5, 0.5, 0.5);
            saveSpawnLocation(this.spawnLocation);
        }
    }

    /**
     * 관리자가 야생 고정 스폰 위치를 새로 설정
     */
    public void setSpawnLocation(Location location) {
        if (location == null || location.getWorld() == null) return;
        this.spawnLocation = location.clone();
        saveSpawnLocation(this.spawnLocation);
    }

    private void saveSpawnLocation(Location loc) {
        if (loc == null) return;
        wildConfig.set("spawn.x", loc.getX());
        wildConfig.set("spawn.y", loc.getY());
        wildConfig.set("spawn.z", loc.getZ());
        wildConfig.set("spawn.yaw", loc.getYaw());
        wildConfig.set("spawn.pitch", loc.getPitch());
        try {
            wildConfig.save(wildConfigFile);
        } catch (IOException e) {
            plugin.getLogger().warning("wildConfig.yml 저장 실패: " + e.getMessage());
        }
    }

    /**
     * 야생 월드인지 확인
     */
    public boolean isWildWorld(World world) {
        if (world == null) return false;
        return world.getName().equalsIgnoreCase(WILD_WORLD_NAME);
    }

    public boolean isWildWorld(Location location) {
        if (location == null) return false;
        return isWildWorld(location.getWorld());
    }

    /**
     * 플레이어를 야생 고정 스폰 지점으로 텔레포트
     */
    public boolean teleportToWild(Player player) {
        if (wildWorld == null) {
            player.sendMessage("§c[MineFarm] 야생 월드가 로드되지 않았습니다.");
            return false;
        }

        Location target = (spawnLocation != null) ? spawnLocation : wildWorld.getSpawnLocation();
        player.closeInventory();
        player.teleport(target);
        player.playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        player.sendMessage("§a[MineFarm] §f야생 월드로 이동했습니다.");
        return true;
    }

    /**
     * 플러그인 비활성화/리로드 시 정리
     */
    public void cleanup() {
        if (itemCleaner != null) {
            itemCleaner.stop();
        }
    }

    public World getWildWorld() {
        return wildWorld;
    }

    public Location getSpawnLocation() {
        return spawnLocation != null ? spawnLocation : (wildWorld != null ? wildWorld.getSpawnLocation() : null);
    }

    public WildItemCleaner getItemCleaner() {
        return itemCleaner;
    }
}
