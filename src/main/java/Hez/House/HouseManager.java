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
}
