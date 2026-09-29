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

    // 파일들
    private File moneyFile;
    private File jobFile;
    private File itemFile;
    private File houseFile;
    private FileConfiguration moneyConfig;
    private FileConfiguration jobConfig;
    private FileConfiguration itemConfig;
    private FileConfiguration houseConfig;
    @Override
    public void onEnable() {
        instance = this;

        // 파일 초기화
        InitFiles();

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

        moneyFile = new File(getDataFolder(), "PlayerMoney.yml");
        jobFile = new File(getDataFolder(), "PlayerJobs.yml");
        itemFile = new File(getDataFolder(), "Item.yml");
        houseFile = new File(getDataFolder(), "house.yml");

        moneyConfig = YamlConfiguration.loadConfiguration(moneyFile);
        jobConfig = YamlConfiguration.loadConfiguration(jobFile);
        itemConfig = YamlConfiguration.loadConfiguration(itemFile);
        houseConfig = YamlConfiguration.loadConfiguration(houseFile);
    }

    private void LoadPapi(){
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new MoneyExpansion().register();
            new JobExpansion().register();
        }
        else{ getLogger().warning("papi실패!");}
    }

    private void LoadData(){

        // 돈 관리 시스템
        this.moneyManager = new MoneyManager();
        this.moneyManager.LoadData(moneyConfig);

        // 직업 관리 시스템
        this.jobManager = new JobManager();
        this.jobManager.LoadData(jobConfig);

        // 물고기 관리 시스템
        this.fishManager = new FishManager();
        this.fishManager.Init();
        getServer().getPluginManager().registerEvents(new FishingListener(), this);

        // 청크 관리 시스템
        this.houseManager = new HouseManager();
        this.houseManager.LoadData(houseConfig);

        // 디스플레이 시스템
        this.displayManager = new DisplayManager(this);
        this.displayManager.InitDisplay();
        this.displayManager.startRaytraceTask();


        // 커맨드 관리 시스템 *** 항상 맨 밑에 두시오 ***
        new CommandManager(this).registerCommands();
    }

    public void SaveData() {
        // 각 메니저에 옮기기
        moneyManager.saveData(moneyConfig);
        jobManager.SaveData(jobConfig);
        houseManager.SaveData(houseConfig);

        // 실제 파일로 덮어쓰기
        try {
            moneyConfig.save(moneyFile);
            jobConfig.save(jobFile);
            houseConfig.save(houseFile);
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
}
