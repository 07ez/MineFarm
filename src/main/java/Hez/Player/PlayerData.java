package Hez.Player;

import Hez.Encyclopedia.PlayerEncyclopedia;

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
        int totalJumps,

        // 도감 데이터 (비트마스크)
        PlayerEncyclopedia encyclopedia
) {
    public PlayerData(UUID playerUUID, int money, List<String> chunkList,
                      int totalCropsHarvest, int totalFishCatch, int totalOreMining, int totalJumps) {
        this(playerUUID, money, chunkList, totalCropsHarvest, totalFishCatch, totalOreMining, totalJumps, new PlayerEncyclopedia());
    }
}
