package Hez.Player;

import Hez.CustomItem.Fish.FishManager;
import Hez.House.ChunkKey;
import Hez.House.HouseManager;
import Hez.MineFarm.Main;
import Hez.Money.MoneyManager;
import org.bukkit.Chunk;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.*;

public class PlayerManager implements Listener {
    private final Map<UUID, PlayerData> playersData = new HashMap<>();

    // 전달해야할 메니저들
    MoneyManager moneyManager;
    HouseManager houseManager;

    // 파일
    FileConfiguration playerData;

    @EventHandler
    public void OnJoin(PlayerJoinEvent event) {
        UUID playerUUID  = event.getPlayer().getUniqueId();
        getOrCreatePlayerData(playerUUID);
        moneyManager.LoadPlayerMoney(playerUUID, 0); // 돈 정보 전달
    }

    public void LoadData(FileConfiguration file) {
        // 각 전달할 메니저들
        moneyManager = Main.getInstance().getMoneyManager();
        houseManager = Main.getInstance().getHouseManager();

        // 기존 데이터 메모리 초기화
        playersData.clear();

        // 데이터 가져오기
        for (String uuidStr : file.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);

                // "UUID.항목" 경로로 접근.
                int money = file.getInt(uuidStr + ".money", 0);
                List<String> chunkKeyList = (file.getStringList(uuidStr + ".chunkKey"));
                List<String> ownedItems = file.getStringList(uuidStr + ".ownedRecipeItems");

                // 레시피 해금용 보유 이력 로드
                Hez.CustomItem.RecipeUntil.loadOwnedItems(uuid, ownedItems);

                // 도감 데이터 로드 (비트마스크)
                Hez.Encyclopedia.PlayerEncyclopedia encyclopedia = new Hez.Encyclopedia.PlayerEncyclopedia();
                encyclopedia.loadMaskList(Hez.Encyclopedia.EncyclopediaType.DEFAULT, file.getIntegerList(uuidStr + ".encyclopedia.default"));
                encyclopedia.loadMaskList(Hez.Encyclopedia.EncyclopediaType.FISH, file.getIntegerList(uuidStr + ".encyclopedia.fish"));
                encyclopedia.loadMaskList(Hez.Encyclopedia.EncyclopediaType.COOK, file.getIntegerList(uuidStr + ".encyclopedia.cook"));

                // 저장
                PlayerData data = new PlayerData(uuid, money, chunkKeyList, 0, 0, 0, 0, encyclopedia);
                playersData.put(uuid, data);

            } catch (IllegalArgumentException e) {
                // 혹시 UUID 형식에 안 맞는 다른 키가 존재하면 예외 처리
            }
        }
    }

    public void saveData(FileConfiguration file) {
        // 덮어쓰기를 위한 초기화
        for (String key : file.getKeys(false)) {
            file.set(key, null);
        }

        // 플레이어 데이터 저장
        for (Map.Entry<UUID, PlayerData> entry : playersData.entrySet()) {
            String uuidStr = entry.getKey().toString();
            PlayerData data = entry.getValue();

            // 저장
            file.set(uuidStr + ".money", data.money()); // 돈
            for (String chunkKey : data.chunkList()) {
                file.set(uuidStr + ".chunkKey", chunkKey); // 보유한 집
            }
            // 통계용
            file.set(uuidStr + ".totalCropsHarvest", data.totalCropsHarvest());
            // 레시피 재료 소유 이력 저장
            file.set(uuidStr + ".ownedRecipeItems", Hez.CustomItem.RecipeUntil.getOwnedItemsList(entry.getKey()));

            // 도감 비트마스크 저장
            if (data.encyclopedia() != null) {
                file.set(uuidStr + ".encyclopedia.default", data.encyclopedia().getMaskList(Hez.Encyclopedia.EncyclopediaType.DEFAULT));
                file.set(uuidStr + ".encyclopedia.fish", data.encyclopedia().getMaskList(Hez.Encyclopedia.EncyclopediaType.FISH));
                file.set(uuidStr + ".encyclopedia.cook", data.encyclopedia().getMaskList(Hez.Encyclopedia.EncyclopediaType.COOK));
            }
        }

        // 접속 중인 플레이어의 레시피 재료 이력도 누락 없이 저장
        for (org.bukkit.entity.Player online : org.bukkit.Bukkit.getOnlinePlayers()) {
            UUID u = online.getUniqueId();
            file.set(u.toString() + ".ownedRecipeItems", Hez.CustomItem.RecipeUntil.getOwnedItemsList(u));
        }
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playersData.get(uuid);
    }

    public PlayerData getOrCreatePlayerData(UUID uuid) {
        return playersData.computeIfAbsent(uuid, u -> new PlayerData(u, 0, new ArrayList<>(), 0, 0, 0, 0, new Hez.Encyclopedia.PlayerEncyclopedia()));
    }
}
