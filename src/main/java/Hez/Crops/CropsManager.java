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

    // 플레이어별 활성 작물 TextDisplay
    public record ActiveCropDisplay(BlockPos pos, org.bukkit.entity.TextDisplay display) {}
    private final Map<UUID, ActiveCropDisplay> activeDisplays = new ConcurrentHashMap<>();

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

    /**
     * 웅크리기 + 우클릭 시 호출:
     * 해당 작물 블록 위에 남은 시간 또는 '수확가능!'을 안내하는 TextDisplay를 소환
     */
    public void showCropDisplay(org.bukkit.entity.Player player, Block block, CropData cropData) {
        // 기존에 이 플레이어가 띄워둔 디스플레이가 있다면 먼저 제거
        removeCropDisplay(player);

        // 시각적 나이 먼저 갱신
        updateBlockVisual(block, cropData);

        Location displayLoc = block.getLocation().add(0.5, 0.75, 0.5);
        org.bukkit.entity.TextDisplay textDisplay = displayLoc.getWorld().spawn(displayLoc, org.bukkit.entity.TextDisplay.class, text -> {
            text.setText(cropData.getFormattedRemainingTime());
            text.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
            text.setSeeThrough(false);
            text.setBackgroundColor(org.bukkit.Color.fromARGB(120, 0, 0, 0));

            org.bukkit.util.Transformation transformation = text.getTransformation();
            transformation.getScale().set(0.6f, 0.6f, 0.6f);
            text.setTransformation(transformation);

            text.setPersistent(false); // 재시작 시 잔존 엔티티 방지
        });

        activeDisplays.put(player.getUniqueId(), new ActiveCropDisplay(BlockPos.from(block), textDisplay));
    }

    /**
     * 특정 플레이어의 활성 작물 TextDisplay 제거
     */
    public void removeCropDisplay(org.bukkit.entity.Player player) {
        ActiveCropDisplay active = activeDisplays.remove(player.getUniqueId());
        if (active != null && active.display().isValid()) {
            active.display().remove();
        }
    }

    /**
     * 크로스헤어 감지 태스크:
     * 플레이어의 시선(크로스헤어)이 작물에서 벗어나면 TextDisplay 즉시 해제,
     * 계속 바라보고 있다면 남은 시간 및 작물 나이를 실시간 갱신
     */
    public void startDisplayWatcherTask(org.bukkit.plugin.java.JavaPlugin plugin) {
        org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (activeDisplays.isEmpty()) return;

            Iterator<Map.Entry<UUID, ActiveCropDisplay>> iterator = activeDisplays.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, ActiveCropDisplay> entry = iterator.next();
                org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(entry.getKey());
                ActiveCropDisplay active = entry.getValue();

                // 플레이어가 접속 종료했거나 사망했거나 엔티티가 유효하지 않으면 정리
                if (player == null || !player.isOnline() || !active.display().isValid()) {
                    if (active != null && active.display().isValid()) {
                        active.display().remove();
                    }
                    iterator.remove();
                    continue;
                }

                // 플레이어 크로스헤어가 작물 블록을 바라보고 있는지 레이트레이스로 검사
                org.bukkit.util.RayTraceResult hit = player.rayTraceBlocks(5.5, org.bukkit.FluidCollisionMode.NEVER);
                boolean lookingAtTarget = false;

                if (hit != null && hit.getHitBlock() != null) {
                    BlockPos hitPos = BlockPos.from(hit.getHitBlock());
                    if (hitPos.equals(active.pos())) {
                        lookingAtTarget = true;
                    }
                }

                if (!lookingAtTarget) {
                    // 크로스헤어가 작물에서 벗어남 -> 즉시 해제
                    active.display().remove();
                    iterator.remove();
                } else {
                    // 여전히 바라보고 있음 -> 실시간 시간 및 나이 동기화
                    CropData data = cropsByPos.get(active.pos());
                    if (data != null && hit.getHitBlock() != null) {
                        updateBlockVisual(hit.getHitBlock(), data);
                        active.display().setText(data.getFormattedRemainingTime());
                    }
                }
            }
        }, 1L, 1L);
    }

    /**
     * 서버 종료/리로드 시 모든 활성 디스플레이 정리
     */
    public void clearAllDisplays() {
        for (ActiveCropDisplay active : activeDisplays.values()) {
            if (active.display() != null && active.display().isValid()) {
                active.display().remove();
            }
        }
        activeDisplays.clear();
    }
}
