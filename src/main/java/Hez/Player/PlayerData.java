package Hez.Player;

import Hez.House.ChunkKey;

import java.util.List;
import java.util.UUID;

public record PlayerData(
        // 플레이어 정보
        UUID playerUUID,

        //
        int money,
        List<String> chunkList,

        // 통계 기록용
        int totalCropsHarvest,
        int totalFishCatch,
        int totalOreMining,
        int totalJumps

        // 퀘스트 기록용
) {
}
