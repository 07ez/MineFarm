package Hez.Quest;

import Hez.MineFarm.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 퀘스트 창의 9개 행(Row 0 ~ Row 8)에 배치될 아이템 열 위치(0~8)를 관리하는 매니저.
 * - 하드코딩 모드 또는 플레이어별 고유 랜덤 시드 기억하기 지원
 * - quest_layout.yml에 영구 저장하여 창을 닫았다 열거나 재접속해도 항상 동일한 배치 유지
 */
public class QuestLayoutManager {

    private static QuestLayoutManager instance;

    // 하드코딩 모드 플래그 (true로 설정 시 HARDCODED_SLOTS 사용)
    public static boolean USE_HARDCODED = false;
    // 하드코딩 시 각 행(0~8)의 기본 열 위치 (0~8)
    public static final int[] HARDCODED_SLOTS = { 4, 2, 7, 1, 5, 3, 8, 0, 6 };

    // 메모리 캐시 (UUID -> 각 행별 선택된 열 번호 9개)
    private final Map<UUID, int[]> playerLayoutMap = new ConcurrentHashMap<>();

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

            // 파일에 저장된 플레이어 레이아웃 메모리에 로드
            if (layoutConfig.contains("players")) {
                for (String uuidStr : layoutConfig.getConfigurationSection("players").getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        List<Integer> slots = layoutConfig.getIntegerList("players." + uuidStr + ".slots");
                        if (slots.size() >= 9) {
                            int[] arr = new int[9];
                            for (int i = 0; i < 9; i++) {
                                arr[i] = slots.get(i);
                            }
                            playerLayoutMap.put(uuid, arr);
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
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
     * 플레이어의 9개 행(Row 0~8) 열 인덱스(0~8) 목록을 가져오거나 새로 생성하여 영구 기억
     * @return 길이 9의 int 배열 (각 원소는 0~8 사이의 열 위치)
     */
    public int[] getOrGenerateSlots(UUID uuid) {
        if (USE_HARDCODED) {
            return HARDCODED_SLOTS.clone();
        }

        // 1. 메모리 캐시 확인
        int[] cached = playerLayoutMap.get(uuid);
        if (cached != null) {
            return cached.clone();
        }

        // 2. 디스크 파일 확인
        synchronized (fileLock) {
            String path = "players." + uuid.toString() + ".slots";
            if (layoutConfig.contains(path)) {
                List<Integer> list = layoutConfig.getIntegerList(path);
                if (list.size() >= 9) {
                    int[] arr = new int[9];
                    for (int i = 0; i < 9; i++) {
                        arr[i] = list.get(i);
                    }
                    playerLayoutMap.put(uuid, arr);
                    return arr.clone();
                }
            }

            // 3. 새로 생성 (랜덤 시드 기반으로 9개 행 각각 0~8 무작위 추첨)
            long seed = new Random().nextLong();
            Random rng = new Random(seed);
            int[] generated = new int[9];
            List<Integer> listToSave = new ArrayList<>(9);
            for (int i = 0; i < 9; i++) {
                int col = rng.nextInt(9);
                generated[i] = col;
                listToSave.add(col);
            }

            // 메모리 및 파일에 영구 저장 (기억하기)
            playerLayoutMap.put(uuid, generated);
            layoutConfig.set("players." + uuid + ".seed", seed);
            layoutConfig.set(path, listToSave);
            saveConfig();

            return generated.clone();
        }
    }

    /**
     * 특정 플레이어의 레이아웃을 강제로 재설정(리롤)할 때 사용
     */
    public void resetLayout(UUID uuid) {
        playerLayoutMap.remove(uuid);
        synchronized (fileLock) {
            layoutConfig.set("players." + uuid.toString(), null);
            saveConfig();
        }
    }
}
