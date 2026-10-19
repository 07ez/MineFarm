package Hez.Quest;

import Hez.MineFarm.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 퀘스트 창의 9개 행(Row 0 ~ Row 8)에 배치될 아이템 열 위치(0~8)를 관리하는 매니저.
 * - 전 플레이어 공통(통일된 위치) 배치 지원
 * - quest_layout.yml의 global.slots에 영구 저장하여 모든 플레이어가 동일한 화면을 보도록 유지
 * - 관리자가 yml 파일에서 슬롯 번호를 직접 수정하거나 하드코딩 모드로 쉽게 변경 가능
 */
public class QuestLayoutManager {

    private static QuestLayoutManager instance;

    // 하드코딩 모드 플래그 (true로 설정 시 HARDCODED_SLOTS 강제 사용)
    public static boolean USE_HARDCODED = false;
    // 하드코딩 시 각 행(0~8)의 기본 열 위치 (0~8)
    public static final int[] HARDCODED_SLOTS = { 4, 2, 7, 1, 5, 3, 8, 0, 6 };

    // 전 플레이어 통일 슬롯 배열 (길이 9)
    private int[] globalSlots = null;

    private final File layoutFile;
    private FileConfiguration layoutConfig;
    private final Object fileLock = new Object();

    public QuestLayoutManager() {
        instance = this;
        File dataFolder = Main.getInstance().getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.layoutFile = new File(dataFolder, "quest_layout.yml");
        loadConfig();
    }

    public static QuestLayoutManager getInstance() {
        if (instance == null) {
            instance = new QuestLayoutManager();
        }
        return instance;
    }

    private void loadConfig() {
        synchronized (fileLock) {
            if (!layoutFile.exists()) {
                try {
                    layoutFile.createNewFile();
                } catch (IOException e) {
                    Main.getInstance().getLogger().severe("quest_layout.yml 생성 실패: " + e.getMessage());
                }
            }
            this.layoutConfig = YamlConfiguration.loadConfiguration(layoutFile);

            // 1. 파일에서 전역 통일 슬롯(global.slots) 로드
            if (layoutConfig.contains("global.slots")) {
                List<Integer> list = layoutConfig.getIntegerList("global.slots");
                if (list.size() >= 9) {
                    this.globalSlots = new int[9];
                    for (int i = 0; i < 9; i++) {
                        this.globalSlots[i] = list.get(i);
                    }
                }
            }

            // 2. 파일에 아직 전역 슬롯이 없다면 최초 1회 랜덤 생성 후 저장 (기억하기)
            if (this.globalSlots == null) {
                long seed = new Random().nextLong();
                Random rng = new Random(seed);
                this.globalSlots = new int[9];
                List<Integer> listToSave = new ArrayList<>(9);
                for (int i = 0; i < 9; i++) {
                    int col = rng.nextInt(9);
                    this.globalSlots[i] = col;
                    listToSave.add(col);
                }
                layoutConfig.set("global.seed", seed);
                layoutConfig.set("global.slots", listToSave);
                saveConfig();
            }
        }
    }

    private void saveConfig() {
        synchronized (fileLock) {
            try {
                layoutConfig.save(layoutFile);
            } catch (IOException e) {
                Main.getInstance().getLogger().severe("quest_layout.yml 저장 실패: " + e.getMessage());
            }
        }
    }

    /**
     * 전 플레이어 공통으로 적용되는 9개 행(Row 0~8) 열 인덱스(0~8) 목록 반환
     * @return 길이 9의 int 배열 (각 원소는 0~8 사이의 열 위치)
     */
    public int[] getSlots() {
        if (USE_HARDCODED) {
            return HARDCODED_SLOTS.clone();
        }
        if (this.globalSlots == null) {
            loadConfig();
        }
        return this.globalSlots.clone();
    }

    /**
     * 하위 호환용 메서드 (모든 플레이어 동일 슬롯 반환)
     */
    public int[] getOrGenerateSlots(UUID uuid) {
        return getSlots();
    }

    /**
     * 전역 슬롯을 새로운 랜덤 시드로 재추첨(리롤)할 때 사용
     */
    public void rerollGlobalSlots() {
        synchronized (fileLock) {
            long seed = new Random().nextLong();
            Random rng = new Random(seed);
            this.globalSlots = new int[9];
            List<Integer> listToSave = new ArrayList<>(9);
            for (int i = 0; i < 9; i++) {
                int col = rng.nextInt(9);
                this.globalSlots[i] = col;
                listToSave.add(col);
            }
            layoutConfig.set("global.seed", seed);
            layoutConfig.set("global.slots", listToSave);
            saveConfig();
        }
    }
}
