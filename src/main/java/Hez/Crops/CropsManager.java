package Hez.Crops;

import Hez.House.ChunkKey;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.CaveVines;
import org.bukkit.block.data.type.CaveVinesPlant;
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

        CropData data = new CropData(pos, type.getBlockMaterial(), now, duration, 1.0f, false, 1);
        cropsByPos.put(pos, data);

        ChunkKey chunkKey = ChunkKey.from(block.getChunk());
        cropsByChunk.computeIfAbsent(chunkKey, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(pos);

        // 초기 나이는 0으로 설정
        if (block.getBlockData() instanceof Ageable ageable) {
            ageable.setAge(0);
            block.setBlockData(ageable, false);
        } else if (type.getHarvestType() == CropType.HarvestType.VINE_RIGHT_CLICK) {
            if (block.getBlockData() instanceof CaveVines caveVines) {
                caveVines.setBerries(false);
                block.setBlockData(caveVines, false);
            }
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
     * 해당 청크에 심겨진 모든 작물의 경과 시간을 계산하여 블록 상태를 즉시 동기화
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
            CropType type = cropData.getCropType();

            // 블록 타입 유효성 검사
            boolean valid = false;
            if (type != null) {
                if (type.getHarvestType() == CropType.HarvestType.BLOCK_TRANSFORM) {
                    valid = (block.getType() == type.getBlockMaterial() || block.getType() == type.getDropMaterial());
                } else if (type.getHarvestType() == CropType.HarvestType.DOUBLE_PLANT) {
                    valid = (block.getType() == type.getBlockMaterial() || block.getType() == type.getDropMaterial());
                } else if (type.getHarvestType() == CropType.HarvestType.CHORUS_TREE) {
                    valid = (block.getType() == Material.CHORUS_FLOWER || block.getType() == Material.CHORUS_PLANT);
                } else if (type.getHarvestType() == CropType.HarvestType.VINE_RIGHT_CLICK) {
                    valid = (block.getType() == Material.CAVE_VINES || block.getType() == Material.CAVE_VINES_PLANT);
                } else {
                    valid = (block.getType() == cropData.getCropMaterial());
                }
            } else {
                valid = (block.getType() == cropData.getCropMaterial());
            }

            if (!valid) {
                toCleanUp.add(pos);
                continue;
            }

            // 실시간 시간 계산 후 시각적 상태 동기화
            updateBlockVisual(block, cropData);
        }

        for (BlockPos cleanupPos : toCleanUp) {
            removeCrop(cleanupPos);
        }
    }

    /**
     * 특정 블록의 시각적 성장 단계(Age, 블록 변환 등)를 실시간 계산값으로 업데이트
     */
    public void updateBlockVisual(Block block, CropData cropData) {
        CropType type = cropData.getCropType();
        if (type == null) {
            // 기본 Ageable 처리
            BlockData blockData = block.getBlockData();
            if (blockData instanceof Ageable ageable) {
                int maxAge = ageable.getMaximumAge();
                int calculatedAge = cropData.calculateAge(maxAge);
                if (ageable.getAge() != calculatedAge) {
                    ageable.setAge(calculatedAge);
                    block.setBlockData(ageable, false);
                }
            }
            return;
        }

        switch (type.getHarvestType()) {
            case BLOCK_TRANSFORM -> {
                // 수박, 호박: 성장 중에는 줄기 나이 증가, 다 자라면 블록 변환
                if (cropData.isHarvestable()) {
                    if (block.getType() != type.getDropMaterial()) {
                        block.setType(type.getDropMaterial(), false);
                    }
                } else {
                    if (block.getBlockData() instanceof Ageable ageable) {
                        int maxAge = ageable.getMaximumAge();
                        int calculatedAge = cropData.calculateAge(maxAge);
                        if (ageable.getAge() != calculatedAge) {
                            ageable.setAge(calculatedAge);
                            block.setBlockData(ageable, false);
                        }
                    }
                }
            }
            case DOUBLE_PLANT -> {
                // 벌레잡이풀: 다 자라면 2칸 식물 생성
                if (cropData.isHarvestable()) {
                    if (block.getType() != Material.PITCHER_PLANT) {
                        Block topBlock = block.getRelative(BlockFace.UP);
                        if (topBlock.getType() == Material.AIR) {
                            block.setType(Material.PITCHER_PLANT, false);
                            topBlock.setType(Material.PITCHER_PLANT, false);

                            if (block.getBlockData() instanceof Bisected lower && topBlock.getBlockData() instanceof Bisected upper) {
                                lower.setHalf(Bisected.Half.BOTTOM);
                                upper.setHalf(Bisected.Half.TOP);
                                block.setBlockData(lower, false);
                                topBlock.setBlockData(upper, false);
                            }
                        }
                    }
                } else {
                    if (block.getBlockData() instanceof Ageable ageable) {
                        int maxAge = ageable.getMaximumAge();
                        int calculatedAge = cropData.calculateAge(maxAge);
                        if (ageable.getAge() != calculatedAge) {
                            ageable.setAge(calculatedAge);
                            block.setBlockData(ageable, false);
                        }
                    }
                }
            }
            case TOWER -> {
                // 사탕수수, 선인장은 최대 3단, 켈프는 최대 15단까지 위로 성장
                int maxStage = (type == CropType.KELP) ? 15 : 3;
                if (cropData.isHarvestable() && cropData.getCurrentStage() < maxStage) {
                    Block nextBlock = block.getRelative(0, cropData.getCurrentStage(), 0);
                    boolean canGrow = false;
                    if (type == CropType.KELP) {
                        canGrow = (nextBlock.getType() == Material.WATER);
                    } else {
                        canGrow = (nextBlock.getType() == Material.AIR);
                    }

                    if (canGrow) {
                        nextBlock.setType(type.getBlockMaterial(), true);
                        cropData.setCurrentStage(cropData.getCurrentStage() + 1);
                        cropData.setPlantedAt(System.currentTimeMillis());
                        cropData.setBoneMealUsed(false);
                    }
                }
            }
            case VINE_RIGHT_CLICK -> {
                // 빛나는 열매: 연결된 모든 덩굴 블록을 검사
                // 1순위: 열매를 맺을 곳(isBerries == false)이 있으면 열매 맺기 (1개만)
                // 2순위: 모든 덩굴에 이미 열매가 맺혀있어 맺을 곳이 없으면 아래로 자라기 (아래가 공기일 때)
                if (cropData.isHarvestable()) {
                    List<Block> connectedVines = getConnectedVineBlocks(block);
                    if (connectedVines.isEmpty()) return;

                    // 열매를 맺을 수 있는 블록(아직 열매가 없는 블록) 탐색
                    Block targetBlock = null;
                    for (Block vine : connectedVines) {
                        if (vine.getBlockData() instanceof CaveVines caveVines) {
                            if (!caveVines.isBerries()) {
                                targetBlock = vine;
                                break;
                            }
                        } else if (vine.getBlockData() instanceof CaveVinesPlant caveVinesPlant) {
                            if (!caveVinesPlant.isBerries()) {
                                targetBlock = vine;
                                break;
                            }
                        }
                    }

                    if (targetBlock != null) {
                        // 열매 맺을 곳이 있으므로 열매 1개 맺기
                        if (targetBlock.getBlockData() instanceof CaveVines caveVines) {
                            caveVines.setBerries(true);
                            targetBlock.setBlockData(caveVines, false);
                        } else if (targetBlock.getBlockData() instanceof CaveVinesPlant caveVinesPlant) {
                            caveVinesPlant.setBerries(true);
                            targetBlock.setBlockData(caveVinesPlant, false);
                        }
                        cropData.setPlantedAt(System.currentTimeMillis());
                        cropData.setBoneMealUsed(false);
                    } else {
                        // 열매 맺을 곳이 없으므로(모든 덩굴에 열매가 있음), 맨 아래에서 아래로 1칸 자라기 시도
                        Block bottomBlock = connectedVines.get(connectedVines.size() - 1);
                        Block below = bottomBlock.getRelative(BlockFace.DOWN);
                        if (below.getType() == Material.AIR) {
                            bottomBlock.setType(Material.CAVE_VINES_PLANT, false);
                            below.setType(Material.CAVE_VINES, false);
                            cropData.setPlantedAt(System.currentTimeMillis());
                            cropData.setBoneMealUsed(false);
                        }
                    }
                }
            }
            case CHORUS_TREE -> {
                // 후렴과: 단계별 가지치기 성장
                if (cropData.isHarvestable()) {
                    if (block.getType() == Material.CHORUS_FLOWER) {
                        Block top = block.getRelative(BlockFace.UP);
                        if (top.getType() == Material.AIR) {
                            block.setType(Material.CHORUS_PLANT, false);
                            top.setType(Material.CHORUS_FLOWER, false);
                            cropData.setCurrentStage(cropData.getCurrentStage() + 1);
                            cropData.setPlantedAt(System.currentTimeMillis());
                            cropData.setBoneMealUsed(false);
                        }
                    }
                }
            }
            case BREAK_CROP -> {
                // 일반 Ageable (감자, 당근, 비트, 밀, 횃불꽃, 달콤한 열매, 코코아 등)
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
     * 타워형 작물(사탕수수, 선인장, 켈프)의 밑동 찾기
     */
    public CropData findTowerBaseCrop(Block block) {
        Block current = block;
        Material mat = block.getType();
        while (current.getY() > current.getWorld().getMinHeight()) {
            Block below = current.getRelative(BlockFace.DOWN);
            if (below.getType() == mat || (mat == Material.KELP && below.getType() == Material.KELP_PLANT) || (mat == Material.KELP_PLANT && below.getType() == Material.KELP)) {
                current = below;
            } else {
                break;
            }
        }
        return getCrop(current.getLocation());
    }

    /**
     * 동굴 덩굴(빛나는 열매)의 루트(천장 밑동) 블록 찾기
     */
    public Block findVineRootBlock(Block block) {
        Block current = block;
        while (current.getY() < current.getWorld().getMaxHeight()) {
            Block above = current.getRelative(BlockFace.UP);
            if (above.getType() == Material.CAVE_VINES || above.getType() == Material.CAVE_VINES_PLANT) {
                current = above;
            } else {
                break;
            }
        }
        return current;
    }

    /**
     * 동굴 덩굴(빛나는 열매)의 루트(천장 밑동) 찾기
     */
    public CropData findVineRootCrop(Block block) {
        Block rootBlock = findVineRootBlock(block);
        return getCrop(rootBlock.getLocation());
    }

    /**
     * 루트 블록으로부터 아래로 연결된 모든 덩굴 블록들을 순서대로 반환
     */
    public List<Block> getConnectedVineBlocks(Block rootBlock) {
        List<Block> list = new ArrayList<>();
        Block curr = rootBlock;
        while (curr.getType() == Material.CAVE_VINES || curr.getType() == Material.CAVE_VINES_PLANT) {
            list.add(curr);
            curr = curr.getRelative(BlockFace.DOWN);
        }
        return list;
    }

    /**
     * 데이터 저장 (cropsData.yml)
     */
    public void saveData(FileConfiguration config) {
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
            config.set(key + ".boneMealUsed", data.isBoneMealUsed());
            config.set(key + ".currentStage", data.getCurrentStage());
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
            boolean boneMealUsed = section.getBoolean(key + ".boneMealUsed", false);
            int currentStage = section.getInt(key + ".currentStage", 1);

            if (world == null || matName == null) continue;

            try {
                Material material = Material.valueOf(matName);
                BlockPos pos = new BlockPos(world, x, y, z);
                CropData cropData = new CropData(pos, material, plantedAt, growDuration, boost, boneMealUsed, currentStage);

                cropsByPos.put(pos, cropData);

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
        removeCropDisplay(player);
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

            text.setPersistent(false);
        });

        activeDisplays.put(player.getUniqueId(), new ActiveCropDisplay(cropData.getPos(), textDisplay));
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
     * 주기적 작물 성장 상태 동기화 태스크 (1초마다)
     */
    public void startCropGrowthUpdaterTask(org.bukkit.plugin.java.JavaPlugin plugin) {
        org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (cropsByPos.isEmpty()) return;

            for (Map.Entry<BlockPos, CropData> entry : cropsByPos.entrySet()) {
                BlockPos pos = entry.getKey();
                CropData cropData = entry.getValue();

                org.bukkit.World world = org.bukkit.Bukkit.getWorld(pos.worldName());
                if (world == null) continue;

                int chunkX = pos.x() >> 4;
                int chunkZ = pos.z() >> 4;
                if (!world.isChunkLoaded(chunkX, chunkZ)) continue;

                Block block = world.getBlockAt(pos.x(), pos.y(), pos.z());
                updateBlockVisual(block, cropData);
            }
        }, 20L, 20L);
    }

    /**
     * 크로스헤어 감지 태스크:
     * 시선이 벗어나면 TextDisplay 해제
     */
    public void startDisplayWatcherTask(org.bukkit.plugin.java.JavaPlugin plugin) {
        org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (activeDisplays.isEmpty()) return;

            Iterator<Map.Entry<UUID, ActiveCropDisplay>> iterator = activeDisplays.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, ActiveCropDisplay> entry = iterator.next();
                org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(entry.getKey());
                ActiveCropDisplay active = entry.getValue();

                if (player == null || !player.isOnline() || !active.display().isValid()) {
                    if (active != null && active.display().isValid()) {
                        active.display().remove();
                    }
                    iterator.remove();
                    continue;
                }

                org.bukkit.util.RayTraceResult hit = player.rayTraceBlocks(5.5, org.bukkit.FluidCollisionMode.NEVER);
                boolean lookingAtTarget = false;

                if (hit != null && hit.getHitBlock() != null) {
                    BlockPos hitPos = BlockPos.from(hit.getHitBlock());
                    // 타워형(사탕수수/선인장/켈프 최대 15단) 또는 덩굴형 위치 매칭
                    if (hitPos.equals(active.pos()) ||
                        (hitPos.worldName().equals(active.pos().worldName()) && hitPos.x() == active.pos().x() && hitPos.z() == active.pos().z() && Math.abs(hitPos.y() - active.pos().y()) <= 15)) {
                        lookingAtTarget = true;
                    }
                }

                if (!lookingAtTarget) {
                    active.display().remove();
                    iterator.remove();
                } else {
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
