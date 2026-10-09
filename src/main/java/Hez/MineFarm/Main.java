package Hez.MineFarm;

import Hez.CustomItem.AddRecipe;
import Hez.CustomItem.Fish.FishManager;
import Hez.CustomItem.Fish.FishingListener;
import Hez.Display.DisplayManager;
import Hez.House.HouseManager;
import Hez.Job.JobManager;
import Hez.Money.MoneyManager;
import Hez.Command.CommandManager;
import Hez.Placeholder.JobExpansion;
import Hez.Placeholder.MoneyExpansion;
import Hez.Player.PlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class Main extends JavaPlugin {
    private static Main instance;

    // 매니저
    private MoneyManager moneyManager;
    private JobManager jobManager;
    private FishManager fishManager;
    private HouseManager houseManager;
    private DisplayManager displayManager;
    private PlayerManager playerManager;
    private Hez.Crops.CropsManager cropsManager;

    // 파일들
    private File playerDataFile;
    private File cropsDataFile;
    private File itemFile;
    private FileConfiguration itemConfig;
    private FileConfiguration playerDataFileConfig;
    private FileConfiguration cropsDataConfig;

    @Override
    public void onEnable() {
        instance = this;

        // 파일 초기화
        InitFiles();

        InitManager();

        // 데이터 로딩
        LoadData();

        // 레시피 등록
        new AddRecipe(this).registarAll();

        // papi로딩
        LoadPapi();
    }

    @Override
    public void onDisable() {
        SaveData();
    }

    private void InitFiles() {
        // 플러그인 데이터 폴더가 없으면 생성
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        playerDataFile = new File(getDataFolder(), "playerData.yml");
        cropsDataFile = new File(getDataFolder(), "cropsData.yml");
        itemFile = new File(getDataFolder(), "Item.yml");
        playerDataFileConfig = YamlConfiguration.loadConfiguration(playerDataFile);
        cropsDataConfig = YamlConfiguration.loadConfiguration(cropsDataFile);
        itemConfig = YamlConfiguration.loadConfiguration(itemFile);
    }

    private void InitManager() {
        // 돈 관리 시스템
        this.moneyManager = new MoneyManager();

        // 직업 관리 시스템
        this.jobManager = new JobManager();

        // 물고기 관리 시스템
        this.fishManager = new FishManager();

        // 청크 관리 시스템
        this.houseManager = new HouseManager();

        // 디스플레이 시스템
        this.displayManager = new DisplayManager(this);

        // 플레이어 데이터 관리 시스템
        this.playerManager = new PlayerManager();

        // 농사 관리 시스템
        this.cropsManager = new Hez.Crops.CropsManager();
    }

    private void LoadPapi(){
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new MoneyExpansion().register();
            new JobExpansion().register();
        }
        else{ getLogger().warning("papi실패!");}
    }

    private void LoadData(){
        // 물고기 등록
        this.fishManager.Init();
        getServer().getPluginManager().registerEvents(new FishingListener(), this);

        // 디스플레이 등록
        this.displayManager.InitDisplay();
        this.displayManager.startRaytraceTask();

        // 농사 데이터 로딩 및 리스너 등록
        this.cropsManager.loadData(cropsDataConfig);
        getServer().getPluginManager().registerEvents(new Hez.Crops.CropListener(this.cropsManager), this);

        // 플레이어 데이터 파일 로딩
        this.playerManager.LoadData(playerDataFileConfig);

        // 커맨드 관리 시스템 *** 항상 맨 밑에 두시오 ***
        new CommandManager(this).registerCommands();
    }

    public void SaveData() {
        // 각 메니저에 옮기기
        playerManager.saveData(playerDataFileConfig);
        cropsManager.saveData(cropsDataConfig);
        // 실제 파일로 덮어쓰기
        try {
            playerDataFileConfig.save(playerDataFile);
            cropsDataConfig.save(cropsDataFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Main getInstance() { return instance; }
    public MoneyManager getMoneyManager() {
        return moneyManager;
    }
    public JobManager getJobManager() { return jobManager; }
    public FishManager getFishManager() { return fishManager; }
    public HouseManager getHouseManager() { return houseManager; }
    public DisplayManager getDisplayManager() { return displayManager; }
    public PlayerManager getPlayerManager() { return playerManager; }
    public Hez.Crops.CropsManager getCropsManager() { return cropsManager; }
}
