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
import Hez.PvP.ColosseumListener;
import Hez.PvP.ColosseumManager;
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
    private Hez.Wilderness.WildManager wildManager;
    private ColosseumManager colosseumManager;

    // 파일들
    private File playerDataFile;
    private File cropsDataFile;
    private File houseDataFile;
    private File itemFile;
    private FileConfiguration itemConfig;
    private FileConfiguration playerDataFileConfig;
    private FileConfiguration cropsDataConfig;
    private FileConfiguration houseDataConfig;

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
        if (this.wildManager != null) {
            this.wildManager.cleanup();
        }
        if (this.colosseumManager != null) {
            this.colosseumManager.cleanup();
        }
        SaveData();
        if (this.cropsManager != null) {
            this.cropsManager.clearAllDisplays();
        }
        if (this.displayManager != null) {
            this.displayManager.clearAllDisplays();
        }
        Hez.CustomItem.Fish.FishingMinigame.clearAll();
        Hez.Quest.QuestInventoryManager.getInstance().restoreAll();
        Hez.Enhance.TridentMinionManager.clearAllMinions();
    }

    private void InitFiles() {
        // 플러그인 데이터 폴더가 없으면 생성
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        playerDataFile = new File(getDataFolder(), "playerData.yml");
        cropsDataFile = new File(getDataFolder(), "cropsData.yml");
        houseDataFile = new File(getDataFolder(), "houseData.yml");
        itemFile = new File(getDataFolder(), "Item.yml");
        playerDataFileConfig = YamlConfiguration.loadConfiguration(playerDataFile);
        cropsDataConfig = YamlConfiguration.loadConfiguration(cropsDataFile);
        houseDataConfig = YamlConfiguration.loadConfiguration(houseDataFile);
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

        // 야생 관리 시스템
        this.wildManager = new Hez.Wilderness.WildManager(this);

        // 콜로세움 PvP 시스템
        this.colosseumManager = new ColosseumManager(this);
    }

    private void LoadPapi(){
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new MoneyExpansion().register();
            new JobExpansion().register();
        }
        else{ getLogger().warning("papi실패!");}
    }

    private void LoadData(){
        // 야생 월드 생성 및 아이템 청소기 초기화
        this.wildManager.init();

        // 물고기 등록
        this.fishManager.Init();
        getServer().getPluginManager().registerEvents(new FishingListener(), this);

        // 디스플레이 등록 및 상점 빠른 거래 리스너 등록
        this.displayManager.InitDisplay();
        this.displayManager.startRaytraceTask();
        getServer().getPluginManager().registerEvents(new Hez.Display.ShopDisplayListener(this, this.displayManager), this);

        // 농사 데이터 로딩 및 리스너 등록
        this.cropsManager.loadData(cropsDataConfig);
        this.cropsManager.startCropGrowthUpdaterTask(this);
        this.cropsManager.startDisplayWatcherTask(this);
        getServer().getPluginManager().registerEvents(new Hez.Crops.CropListener(this.cropsManager), this);

        // 토지 데이터 로딩 및 프로텍터 리스너 등록
        this.houseManager.loadData(houseDataConfig);
        getServer().getPluginManager().registerEvents(new Hez.House.HouseProtector(this.houseManager), this);

        // 메뉴 리스너 등록 (웅크리기 + F키)
        getServer().getPluginManager().registerEvents(new Hez.Menu.MenuListener(), this);

        // 커스텀 요리 섭취 및 레시피 자동 해금 리스너 등록
        getServer().getPluginManager().registerEvents(new Hez.CustomItem.FoodListener(), this);
        getServer().getPluginManager().registerEvents(new Hez.CustomItem.RecipeUntil(), this);

        // 강화 도끼 찹트리, 강화 곡괭이 범위 채굴, 강화 창 방어관통/출혈, 강화 활 속사/멀티샷, 강화 삼지창 미니언, 강화 철퇴 도약강타, 강화대 디스플레이 리스너 등록
        getServer().getPluginManager().registerEvents(new Hez.Enhance.ChopTreeListener(), this);
        getServer().getPluginManager().registerEvents(new Hez.Enhance.AreaMiningListener(), this);
        getServer().getPluginManager().registerEvents(new Hez.Enhance.SpearAttackListener(), this);
        getServer().getPluginManager().registerEvents(new Hez.Enhance.BowShootListener(), this);
        getServer().getPluginManager().registerEvents(new Hez.Enhance.TridentMinionListener(), this);
        getServer().getPluginManager().registerEvents(new Hez.Enhance.MaceSmashListener(), this);
        getServer().getPluginManager().registerEvents(new Hez.Enhance.EnhanceDisplayListener(), this);
        Hez.Enhance.TridentMinionManager.startTask();

        // 플레이어 데이터 파일 로딩 및 접속 리스너 등록
        this.playerManager.LoadData(playerDataFileConfig);
        getServer().getPluginManager().registerEvents(this.playerManager, this);

        // 도감 리스트 및 리스너 등록
        Hez.Encyclopedia.DefaultEncyclopediaRegistry.init();
        getServer().getPluginManager().registerEvents(new Hez.Encyclopedia.EncyclopediaListener(), this);

        // 퀘스트 인벤토리 백업/복원 및 안전 리스너 등록
        getServer().getPluginManager().registerEvents(Hez.Quest.QuestInventoryManager.getInstance(), this);

        // 퀘스트 데이터(quests.json) 로드
        Hez.Quest.QuestManager.getInstance().loadQuestsJson();

        // 콜로세움 PvP 시스템 초기화 및 리스너 등록
        this.colosseumManager.init();
        getServer().getPluginManager().registerEvents(new ColosseumListener(this.colosseumManager), this);

        // 커맨드 관리 시스템 *** 항상 맨 밑에 두시오 ***
        new CommandManager(this).registerCommands();
    }

    public void SaveData() {
        // 각 메니저에 옮기기
        playerManager.saveData(playerDataFileConfig);
        cropsManager.saveData(cropsDataConfig);
        houseManager.saveData(houseDataConfig);
        // 실제 파일로 덮어쓰기
        try {
            playerDataFileConfig.save(playerDataFile);
            cropsDataConfig.save(cropsDataFile);
            houseDataConfig.save(houseDataFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 플러그인의 모든 데이터, 설정, 리스너, 스케줄러 태스크, 레시피를 안전하게 전체 리로드합니다.
     */
    public void reloadPlugin() {
        if (this.wildManager != null) {
            this.wildManager.cleanup();
        }
        if (this.colosseumManager != null) {
            this.colosseumManager.cleanup();
        }

        // 1. 현재 데이터 파일로 안전하게 저장
        SaveData();

        // 1-1. 퀘스트 창을 열고 있는 플레이어 인벤토리 안전 복원
        Hez.Quest.QuestInventoryManager.getInstance().restoreAll();

        // 2. 디스플레이 엔티티 및 미니게임 임시 데이터 정리
        if (this.cropsManager != null) {
            this.cropsManager.clearAllDisplays();
        }
        if (this.displayManager != null) {
            this.displayManager.clearAllDisplays();
        }
        Hez.CustomItem.Fish.FishingMinigame.clearAll();

        // 3. 실행 중인 플러그인 스케줄러 태스크 모두 취소
        Bukkit.getScheduler().cancelTasks(this);

        // 4. 등록된 모든 이벤트 리스너 해제 (중복 등록 방지)
        org.bukkit.event.HandlerList.unregisterAll(this);

        // 5. 등록된 커스텀 레시피 및 캐시 초기화
        Hez.CustomItem.RecipeUntil.resetRecipes();

        // 6. 설정 및 데이터 파일 다시 로드
        InitFiles();

        // 7. 매니저 새로 초기화
        InitManager();

        // 8. 데이터 로딩, 리스너 재등록, 태스크 재시작, 커맨드 재바인딩
        LoadData();

        // 9. 레시피 재등록
        new AddRecipe(this).registarAll();

        // 10. 현재 접속 중인 온라인 플레이어 상태 재동기화
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            java.util.UUID uuid = player.getUniqueId();
            Hez.Player.PlayerData data = playerManager.getOrCreatePlayerData(uuid);
            if (this.moneyManager != null && data != null) {
                this.moneyManager.LoadPlayerMoney(uuid, data.money());
            }
            Hez.CustomItem.RecipeUntil.scanInventoryAndUnlock(player);
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
    public Hez.Wilderness.WildManager getWildManager() { return wildManager; }
    public ColosseumManager getColosseumManager() { return colosseumManager; }
}
