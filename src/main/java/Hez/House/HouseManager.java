package Hez.House;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class HouseManager {
    UUID serverUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    // 땅주인이 누구인지 찾기
    private final Map<ChunkKey, HouseData> houseByChunk = new HashMap<>();
    public boolean isAlreadyHasOwner(ChunkKey chunkKey){ return houseByChunk.containsKey(chunkKey); }

    // 청크 땅 주인 입히기
    public boolean AddHouseChuck(UUID uuid, Chunk chunk) {
        ChunkKey chunkKey = ChunkKey.from(chunk);
        UUID owner = uuid;

        // 땅 주인이 이미 존재하는지 확인
        if(isAlreadyHasOwner(chunkKey)) {
            return false;
        }

        HouseData house = new HouseData(owner);
        house.AddChunk(chunkKey);
        return true;
    }

    // 청크 주인 삭제
    public boolean RemoveChunk(UUID uuid, Chunk chunk) {
        ChunkKey chunkKey = ChunkKey.from(chunk);
        UUID owner = uuid;

        // 땅 주인이 맞는지 확인
        HouseData house = houseByChunk.get(chunkKey);
        if(house != null && house.getOwner().equals(uuid)) { // 땅주인이 맞다면
            house.RemoveChunk(chunkKey);
            houseByChunk.remove(chunkKey);
            return true;
        }

        return false;
    }

    // 청크 서버땅으로 설정
    public boolean SetServerChunk(Chunk chunk) {
        return AddHouseChuck(serverUUID, chunk);
    }

    // 청크 서버땅에서 제외
    public boolean RemoveServerChunk(Chunk chunk) {
        return RemoveChunk(serverUUID, chunk);
    }

    // 청크 정보 불러오기
    public HouseData GetHouseData(Location location) {
        ChunkKey chunkKey = ChunkKey.from(location.getChunk());
        return houseByChunk.get(chunkKey);
    }

    // 권한이 있는지 확인하기
    public boolean hasPermission(UUID uuid, Location location)
    {
        HouseData house = GetHouseData(location);
        if(house == null) return true; // 주인이 없는경우
        return house.getOwner() == uuid || house.getMembers().contains(uuid);
    }

    public void SaveData(FileConfiguration config) {
        // 기존 houses 노드 데이터를 깨끗이 비우고 새로 최신화
        config.set("houses", null);

        // 메모리의 HouseData들을 Owner(UUID) 기준으로 임시 묶음
        Map<UUID, HouseData> ownerMap = new HashMap<>();
        for (HouseData house : houseByChunk.values()) {
            ownerMap.put(house.getOwner(), house);
        }

        // UUID 단위로 YML 노드 데이터 작성
        for (Map.Entry<UUID, HouseData> entry : ownerMap.entrySet()) {
            UUID ownerUuid = entry.getKey();
            HouseData house = entry.getValue();

            String path = "houses." + ownerUuid.toString();

            // 청크 키 목록 저장
            config.set(path + ".chunks", new ArrayList<>(house.getChunks()));

            // 멤버 UUID 목록 저장 (String으로 변환)
            List<String> memberList = new ArrayList<>();
            for (UUID memberUuid : house.getMembers()) {
                memberList.add(memberUuid.toString());
            }
            config.set(path + ".members", memberList);
        }
    }

    public void LoadData(FileConfiguration config) {
        houseByChunk.clear(); // 기존 메모리 초기화

        if (!config.contains("houses")) return;

        var housesSection = config.getConfigurationSection("houses");
        if (housesSection == null) return;

        for (String ownerUuidStr : housesSection.getKeys(false)) {
            try {
                UUID ownerUuid = UUID.fromString(ownerUuidStr);
                String path = "houses." + ownerUuidStr;

                List<String> chunkKeyStrings = config.getStringList(path + ".chunks");
                List<String> memberUuidStrs = config.getStringList(path + ".members");

                HouseData houseData = new HouseData(ownerUuid);

                // 1. 멤버 복원
                for (String memberStr : memberUuidStrs) {
                    houseData.getMembers().add(UUID.fromString(memberStr));
                }

                // 2. 청크 복원 (String -> ChunkKey 객체 변환)
                for (String chunkStr : chunkKeyStrings) {
                    ChunkKey chunkKey = ChunkKey.parse(chunkStr);

                    if (chunkKey != null) {
                        houseData.getChunks().add(chunkKey);      // HouseData 내부의 Set<ChunkKey>에 추가
                        houseByChunk.put(chunkKey, houseData);     // Map<ChunkKey, HouseData>에 등록
                    }
                }

            } catch (IllegalArgumentException e) {
                // UUID 변환 실패 시 스킵
            }
        }

    }
    // 인터랙션 금지
    // 블럭 부수기 금지
}
