package Hez.Crops;

import Hez.House.ChunkKey;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CropsManager {

    // 좌표별 작물 데이터
    private final Map<BlockPos, CropData> cropsByPos = new ConcurrentHashMap<>();
    // 청크별 작물 위치 인덱스
    private final Map<ChunkKey, Set<BlockPos>> cropsByChunk = new ConcurrentHashMap<>();

    /**
     * 작물 등록
     */
    public boolean registerCrop(Block block, Material seedMaterial) {
        CropType type = CropType.fromSeed(seedMaterial);
        if (type == null) {
            type = CropType.fromBlock(block.getType());
        }
        if (type == null) return false;

        BlockPos pos = BlockPos.from(block);
        long now = System.currentTimeMillis();
        long duration = type.getDefaultGrowDurationMs();

        CropData data = new CropData(pos, type.getBlockMaterial(), now, duration);
        cropsByPos.put(pos, data);

        ChunkKey chunkKey = ChunkKey.from(block.getChunk());
        cropsByChunk.computeIfAbsent(chunkKey, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(pos);

        // 초기 나이는 0으로 설정
        if (block.getBlockData() instanceof Ageable ageable) {
            ageable.setAge(0);
            block.setBlockData(ageable, false);
        }

        return true;
    }

    /**
     * 작물 제거
     */
    public CropData removeCrop(BlockPos pos) {
        CropData data = cropsByPos.remove(pos);
        if (data != null) {
            // 청크 인덱스에서도 제거
            for (Map.Entry<ChunkKey, Set<BlockPos>> entry : cropsByChunk.entrySet()) {
                if (entry.getValue().remove(pos)) {
                    if (entry.getValue().isEmpty()) {
                        cropsByChunk.remove(entry.getKey());
                    }
                    break;
                }
            }
        }
        return data;
    }

    public CropData removeCrop(Location loc) {
        return removeCrop(BlockPos.from(loc));
    }

    public CropData getCrop(BlockPos pos) {
        return cropsByPos.get(pos);
    }

    public CropData getCrop(Location loc) {
        return getCrop(BlockPos.from(loc));
    }

    public boolean isManagedCrop(BlockPos pos) {
        return cropsByPos.containsKey(pos);
    }

    public boolean isManagedCrop(Location loc) {
        return isManagedCrop(BlockPos.from(loc));
    }

    /**
     * 청크가 로딩되었을 때 호출:
     * 해당 청크에 심겨진 모든 작물의 경과 시간을 계산하여 블록 상태(Age)를 즉시 동기화
     */
    public void onChunkLoad(Chunk chunk) {
        ChunkKey chunkKey = ChunkKey.from(chunk);
        Set<BlockPos> positions = cropsByChunk.get(chunkKey);
        if (positions == null || positions.isEmpty()) return;

        List<BlockPos> toCleanUp = new ArrayList<>();

        for (BlockPos pos : positions) {
            CropData cropData = cropsByPos.get(pos);
            if (cropData == null) {
                toCleanUp.add(pos);
                continue;
            }

            Block block = chunk.getWorld().getBlockAt(pos.x(), pos.y(), pos.z());

            // 블록이 외부 요인(물, 폭발 등)으로 다른 블록이 된 경우 데이터 정리
            if (block.getType() != cropData.getCropMaterial()) {
                toCleanUp.add(pos);
                continue;
            }

            // 실시간 시간 계산 후 나이 동기화
            updateBlockVisual(block, cropData);
        }

        for (BlockPos cleanupPos : toCleanUp) {
            removeCrop(cleanupPos);
        }
    }

    /**
     * 특정 블록의 시각적 성장 단계(Age)를 실시간 계산값으로 업데이트
     */
    public void updateBlockVisual(Block block, CropData cropData) {
        BlockData blockData = block.getBlockData();
        if (blockData instanceof Ageable ageable) {
            int maxAge = ageable.getMaximumAge();
            int calculatedAge = cropData.calculateAge(maxAge);

            if (ageable.getAge() != calculatedAge) {
                ageable.setAge(calculatedAge);
                block.setBlockData(ageable, false);
            }
        }
    }

    /**
     * 특정 위치의 블록 상태를 갱신하고 CropData 반환
     */
    public CropData syncAndGetCrop(Block block) {
        CropData data = getCrop(block.getLocation());
        if (data != null) {
            updateBlockVisual(block, data);
        }
        return data;
    }

    /**
     * 데이터 저장 (cropsData.yml)
     */
    public void saveData(FileConfiguration config) {
        // 기존 섹션 초기화
        config.set("crops", null);

        for (Map.Entry<BlockPos, CropData> entry : cropsByPos.entrySet()) {
            String key = "crops." + entry.getKey().toString().replace(",", "_");
            CropData data = entry.getValue();

            config.set(key + ".world", data.getPos().worldName());
            config.set(key + ".x", data.getPos().x());
            config.set(key + ".y", data.getPos().y());
            config.set(key + ".z", data.getPos().z());
            config.set(key + ".material", data.getCropMaterial().name());
            config.set(key + ".plantedAt", data.getPlantedAt());
            config.set(key + ".growDuration", data.getGrowDurationMs());
            config.set(key + ".boost", data.getBoost());
        }
    }

    /**
     * 데이터 로드 (cropsData.yml)
     */
    public void loadData(FileConfiguration config) {
        cropsByPos.clear();
        cropsByChunk.clear();

        ConfigurationSection section = config.getConfigurationSection("crops");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String world = section.getString(key + ".world");
            int x = section.getInt(key + ".x");
            int y = section.getInt(key + ".y");
            int z = section.getInt(key + ".z");
            String matName = section.getString(key + ".material");
            long plantedAt = section.getLong(key + ".plantedAt");
            long growDuration = section.getLong(key + ".growDuration");
            float boost = (float) section.getDouble(key + ".boost", 1.0);

            if (world == null || matName == null) continue;

            try {
                Material material = Material.valueOf(matName);
                BlockPos pos = new BlockPos(world, x, y, z);
                CropData cropData = new CropData(pos, material, plantedAt, growDuration, boost);

                cropsByPos.put(pos, cropData);

                // chunkKey: x >> 4, z >> 4
                ChunkKey chunkKey = new ChunkKey(world, x >> 4, z >> 4);
                cropsByChunk.computeIfAbsent(chunkKey, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(pos);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }
}
