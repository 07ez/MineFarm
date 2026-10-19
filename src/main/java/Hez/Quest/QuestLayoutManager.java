package Hez.Quest;

import Hez.MineFarm.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 퀘스트 창의 9개 행(Row 0 ~ Row 8)에 배치될 아이템 열 위치(0~8)를 페이지별로 관리하는 매니저.
 * - 3개 페이지(1, 2, 3페이지) 지원
 * - 전 플레이어 공통(통일된 위치) 배치 지원
 * - quest_layout.yml의 global.pages.<page>.slots에 영구 저장
 */
public class QuestLayoutManager {

    private static QuestLayoutManager instance;

    public static final int MAX_PAGE = 3;

    // 하드코딩 모드 플래그 (true로 설정 시 HARDCODED_PAGES 사용)
    public static boolean USE_HARDCODED = false;
    // 하드코딩 시 각 페이지(1~3)별 9개 행 기본 열 위치 (0~8)
    public static final Map<Integer, int[]> HARDCODED_PAGES = Map.of(
            1, new int[]{ 4, 2, 7, 1, 5, 3, 8, 0, 6 },
            2, new int[]{ 2, 6, 1, 7, 3, 8, 0, 5, 4 },
            3, new int[]{ 6, 1, 5, 2, 7, 4, 3, 8, 0 }
    );

    // 페이지별 통일 슬롯 배열 (Page -> 길이 9 int[])
    private final Map<Integer, int[]> pageSlotsMap = new ConcurrentHashMap<>();

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

            boolean needSave = false;

            // 1~MAX_PAGE(3) 각 페이지별 슬롯 로드 또는 최초 랜덤 생성
            for (int p = 1; p <= MAX_PAGE; p++) {
                String path = "global.pages." + p + ".slots";
                if (layoutConfig.contains(path)) {
                    List<Integer> list = layoutConfig.getIntegerList(path);
                    if (list.size() >= 9) {
                        int[] arr = new int[9];
                        for (int i = 0; i < 9; i++) {
                            arr[i] = list.get(i);
                        }
                        pageSlotsMap.put(p, arr);
                        continue;
                    }
                }

                // 기존 단일 global.slots가 있고 p == 1인 경우 마이그레이션
                if (p == 1 && layoutConfig.contains("global.slots")) {
                    List<Integer> list = layoutConfig.getIntegerList("global.slots");
                    if (list.size() >= 9) {
                        int[] arr = new int[9];
                        for (int i = 0; i < 9; i++) {
                            arr[i] = list.get(i);
                        }
                        pageSlotsMap.put(p, arr);
                        layoutConfig.set(path, list);
                        needSave = true;
                        continue;
                    }
                }

                // 해당 페이지 슬롯이 없으면 랜덤 9개 열(0~8) 생성 후 저장
                long seed = new Random().nextLong();
                Random rng = new Random(seed);
                int[] generated = new int[9];
                List<Integer> listToSave = new ArrayList<>(9);
                for (int i = 0; i < 9; i++) {
                    int col = rng.nextInt(9);
                    generated[i] = col;
                    listToSave.add(col);
                }
                pageSlotsMap.put(p, generated);
                layoutConfig.set("global.pages." + p + ".seed", seed);
                layoutConfig.set(path, listToSave);
                needSave = true;
            }

            if (needSave) {
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
     * 특정 페이지(1~3)의 9개 행(Row 0~8) 열 인덱스(0~8) 목록 반환
     */
    public int[] getSlots(int page) {
        int validPage = Math.max(1, Math.min(MAX_PAGE, page));
        if (USE_HARDCODED) {
            int[] hardcoded = HARDCODED_PAGES.get(validPage);
            if (hardcoded != null) return hardcoded.clone();
        }
        int[] slots = pageSlotsMap.get(validPage);
        if (slots == null) {
            loadConfig();
            slots = pageSlotsMap.get(validPage);
        }
        return (slots != null) ? slots.clone() : new int[]{ 4, 2, 7, 1, 5, 3, 8, 0, 6 };
    }

    /**
     * 1페이지 슬롯 반환 (기본값)
     */
    public int[] getSlots() {
        return getSlots(1);
    }

    /**
     * 하위 호환용 메서드
     */
    public int[] getOrGenerateSlots(UUID uuid) {
        return getSlots(1);
    }
}
