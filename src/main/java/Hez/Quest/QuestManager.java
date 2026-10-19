package Hez.Quest;

import Hez.MineFarm.Main;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * quests.json 퀘스트 데이터 로딩 및 플레이어별 퀘스트 진행 상태를 관리하는 매니저
 */
public class QuestManager {

    private static QuestManager instance;

    // 퀘스트 ID -> QuestData
    private final Map<Integer, QuestData> questMap = new LinkedHashMap<>();
    // "page_row" -> QuestData
    private final Map<String, QuestData> questByPageRowMap = new HashMap<>();

    // 플레이어별 진행 상태 (UUID -> (questId -> QuestStatus))
    private final Map<UUID, Map<Integer, QuestStatus>> playerStatusMap = new ConcurrentHashMap<>();

    private final File questsFile;
    private final File playerStatusFile;
    private FileConfiguration statusConfig;
    private final Object fileLock = new Object();

    public QuestManager() {
        instance = this;
        File dataFolder = Main.getInstance().getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.questsFile = new File(dataFolder, "quests.json");
        this.playerStatusFile = new File(dataFolder, "quest_player_status.yml");

        loadQuestsJson();
        loadPlayerStatus();
    }

    public static QuestManager getInstance() {
        if (instance == null) {
            instance = new QuestManager();
        }
        return instance;
    }

    /**
     * quests.json 파일 로드 (서버 폴더에 없으면 resources에서 복사)
     */
    public void loadQuestsJson() {
        questMap.clear();
        questByPageRowMap.clear();

        if (!questsFile.exists()) {
            try {
                Main.getInstance().saveResource("quests.json", false);
            } catch (Exception e) {
                Main.getInstance().getLogger().warning("quests.json 리소스 복사 실패: " + e.getMessage());
            }
        }

        if (!questsFile.exists()) {
            Main.getInstance().getLogger().warning("quests.json 파일이 존재하지 않습니다.");
            return;
        }

        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(questsFile), StandardCharsets.UTF_8)) {
            JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
            for (JsonElement elem : array) {
                JsonObject obj = elem.getAsJsonObject();
                int id = obj.get("id").getAsInt();
                int page = obj.get("page").getAsInt();
                int row = obj.get("row").getAsInt();
                String title = obj.get("title").getAsString();
                List<String> objectives = new ArrayList<>();
                if (obj.has("objectives")) {
                    for (JsonElement o : obj.getAsJsonArray("objectives")) {
                        objectives.add(o.getAsString());
                    }
                }
                QuestData data = new QuestData(id, page, row, title, objectives);
                questMap.put(id, data);
                questByPageRowMap.put(page + "_" + row, data);
            }
            Main.getInstance().getLogger().info("[Quest] quests.json 로드 완료: 총 " + questMap.size() + "개 퀘스트");
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("[Quest] quests.json 파싱 실패: " + e.getMessage());
        }
    }

    /**
     * 플레이어별 퀘스트 상태 파일(quest_player_status.yml) 로드
     */
    private void loadPlayerStatus() {
        synchronized (fileLock) {
            if (!playerStatusFile.exists()) {
                try {
                    playerStatusFile.createNewFile();
                } catch (IOException e) {
                    Main.getInstance().getLogger().severe("quest_player_status.yml 생성 실패: " + e.getMessage());
                }
            }
            this.statusConfig = YamlConfiguration.loadConfiguration(playerStatusFile);

            if (statusConfig.contains("players")) {
                for (String uuidStr : statusConfig.getConfigurationSection("players").getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        Map<Integer, QuestStatus> map = new HashMap<>();
                        for (String qIdStr : statusConfig.getConfigurationSection("players." + uuidStr).getKeys(false)) {
                            try {
                                int qId = Integer.parseInt(qIdStr);
                                String statusStr = statusConfig.getString("players." + uuidStr + "." + qIdStr, "LOCKED");
                                map.put(qId, QuestStatus.valueOf(statusStr));
                            } catch (Exception ignored) {
                            }
                        }
                        playerStatusMap.put(uuid, map);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }
    }

    private void savePlayerStatus() {
        synchronized (fileLock) {
            try {
                statusConfig.save(playerStatusFile);
            } catch (IOException e) {
                Main.getInstance().getLogger().severe("quest_player_status.yml 저장 실패: " + e.getMessage());
            }
        }
    }

    /**
     * 특정 페이지와 행에 매핑된 퀘스트 데이터 반환
     */
    public QuestData getQuest(int page, int row) {
        return questByPageRowMap.get(page + "_" + row);
    }

    /**
     * 플레이어의 퀘스트 상태 조회 (없으면 기본값: 1번은 IN_PROGRESS, 나머지는 LOCKED)
     */
    public QuestStatus getQuestStatus(UUID uuid, int questId) {
        Map<Integer, QuestStatus> map = playerStatusMap.get(uuid);
        if (map != null && map.containsKey(questId)) {
            return map.get(questId);
        }
        // 기본 상태: 1번 퀘스트는 진행중, 나머지는 잠김
        return (questId == 1) ? QuestStatus.IN_PROGRESS : QuestStatus.LOCKED;
    }

    /**
     * 플레이어의 퀘스트 상태 변경 및 저장
     */
    public void setQuestStatus(UUID uuid, int questId, QuestStatus status) {
        playerStatusMap.computeIfAbsent(uuid, k -> new HashMap<>()).put(questId, status);
        synchronized (fileLock) {
            statusConfig.set("players." + uuid.toString() + "." + questId, status.name());
            savePlayerStatus();
        }
    }
}
