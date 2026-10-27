package Hez.Crops;

import Hez.MineFarm.Main;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.CaveVines;
import org.bukkit.block.data.type.CaveVinesPlant;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class CropListener implements Listener {

    private final CropsManager cropsManager;

    public CropListener(CropsManager cropsManager) {
        this.cropsManager = cropsManager;
    }

    /**
     * 청크 로딩 시:
     * 해당 청크의 작물 데이터를 실시간 시간 기준으로 계산하여 블록 상태를 갱신
     */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event.getWorld().getName().equalsIgnoreCase("colosseum")) return;
        cropsManager.onChunkLoad(event.getChunk());
    }

    /**
     * 작물 씨앗/블록 설치 시 등록
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block placedBlock = event.getBlockPlaced();
        if (placedBlock.getWorld().getName().equalsIgnoreCase("colosseum")) return;
        ItemStack itemInHand = event.getItemInHand();

        CropType type = CropType.fromSeed(itemInHand.getType());
        if (type == null) {
            type = CropType.fromBlock(placedBlock.getType());
        }

        if (type != null) {
            // 타워형(사탕수수, 선인장, 켈프)의 경우: 아래에 이미 밑동이 있는지 검사
            if (type.getHarvestType() == CropType.HarvestType.TOWER) {
                CropData baseCrop = cropsManager.findTowerBaseCrop(placedBlock.getRelative(BlockFace.DOWN));
                if (baseCrop != null) {
                    // 이미 밑동이 존재함: 높이만 업데이트
                    int currentHeight = placedBlock.getY() - baseCrop.getPos().y() + 1;
                    baseCrop.setCurrentStage(Math.max(baseCrop.getCurrentStage(), currentHeight));
                    return;
                }
            }

            cropsManager.registerCrop(placedBlock, itemInHand.getType());
        }
    }

    /**
     * 바닐라 랜덤 틱 자연 성장 전면 차단 (실시간 시간 기반 성장만 허용)
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        if (event.getBlock().getWorld().getName().equalsIgnoreCase("colosseum")) return;
        Block block = event.getBlock();
        if (cropsManager.isManagedCrop(block.getLocation()) ||
            cropsManager.findTowerBaseCrop(block) != null ||
            cropsManager.findVineRootCrop(block) != null ||
            CropType.fromBlock(block.getType()) != null) {
            event.setCancelled(true);
        }
    }

    /**
     * 작물 파괴 및 수확 처리
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getWorld().getName().equalsIgnoreCase("colosseum")) return;

        Player player = event.getPlayer();
        CropData data = cropsManager.getCrop(block.getLocation());

        // 1. 동굴 덩굴 (빛나는 열매) 파괴 처리
        if (block.getType() == Material.CAVE_VINES || block.getType() == Material.CAVE_VINES_PLANT) {
            CropData vineRoot = cropsManager.findVineRootCrop(block);
            if (vineRoot != null) {
                event.setDropItems(false);

                // 캔 블록에 열매가 있었는지 확인
                boolean hasBerries = false;
                if (block.getBlockData() instanceof CaveVines caveVines) {
                    hasBerries = caveVines.isBerries();
                } else if (block.getBlockData() instanceof CaveVinesPlant plant) {
                    hasBerries = plant.isBerries();
                }

                if (hasBerries && player.getGameMode() != GameMode.CREATIVE) {
                    block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.2, 0.5), new ItemStack(Material.GLOW_BERRIES, 1));
                }

                boolean isRoot = vineRoot.getPos().equals(BlockPos.from(block));
                if (isRoot) {
                    // 천장 루트 자체를 캤을 경우: 전체 제거
                    if (!hasBerries && player.getGameMode() != GameMode.CREATIVE) {
                        block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.2, 0.5), new ItemStack(Material.GLOW_BERRIES, 1));
                        player.sendMessage("§7[농사] §f아직 덜 자란 작물을 수확하여 씨앗 1개를 회수했습니다.");
                    }
                    cropsManager.removeCrop(vineRoot.getPos());
                } else {
                    // 중간 또는 아래 덩굴을 캤을 경우: 천장 루트는 계속 보존하여 윗부분 덩굴이 계속 정상 동작하도록 유지
                    vineRoot.setPlantedAt(System.currentTimeMillis());
                    vineRoot.setBoneMealUsed(false);
                }
                return;
            }
        }

        // 2. 타워형 (사탕수수, 선인장, 켈프) 처리
        CropData towerBase = null;
        if (data == null) {
            towerBase = cropsManager.findTowerBaseCrop(block);
        }

        // 벌레잡이풀 2칸 작물 상단 파괴 처리
        if (data == null && block.getType() == Material.PITCHER_PLANT) {
            Block below = block.getRelative(BlockFace.DOWN);
            data = cropsManager.getCrop(below.getLocation());
        }

        if (data == null && towerBase == null) return;

        // 타워형의 윗단 파괴 처리 (밑동은 유지)
        if (towerBase != null && data == null) {
            event.setDropItems(false);
            CropType type = towerBase.getCropType();
            if (type != null && player.getGameMode() != GameMode.CREATIVE) {
                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.2, 0.5), new ItemStack(type.getDropMaterial(), 1));
            }
            int remainingHeight = block.getY() - towerBase.getPos().y();
            towerBase.setCurrentStage(Math.max(1, remainingHeight));
            towerBase.setPlantedAt(System.currentTimeMillis());
            towerBase.setBoneMealUsed(false);
            return;
        }

        // 일반 작물 및 타워 밑동 파괴 처리
        event.setDropItems(false); // 바닐라 기본 드롭(씨앗, 기본아이템) 제거

        cropsManager.updateBlockVisual(block, data);
        CropType type = data.getCropType();
        boolean isHarvestable = data.isHarvestable();

        // 1. 완숙 상태 수확
        if (isHarvestable && type != null) {
            if (player.getGameMode() != GameMode.CREATIVE) {
                int dropAmount = type.getMinDrop();
                if (type.getMaxDrop() > type.getMinDrop()) {
                    dropAmount = ThreadLocalRandom.current().nextInt(type.getMinDrop(), type.getMaxDrop() + 1);
                }

                Location dropLoc = block.getLocation().add(0.5, 0.2, 0.5);
                block.getWorld().dropItemNaturally(dropLoc, new ItemStack(type.getDropMaterial(), dropAmount));
            }

            // 벌레잡이풀 상단 블록 정리
            if (type == CropType.PITCHER_PLANT) {
                Block top = block.getRelative(BlockFace.UP);
                if (top.getType() == Material.PITCHER_PLANT) {
                    top.setType(Material.AIR, false);
                }
            }
        }
        // 2. 미성숙 상태 파괴 -> 심었던 씨앗 1개 반환
        else if (type != null) {
            if (player.getGameMode() != GameMode.CREATIVE) {
                Location dropLoc = block.getLocation().add(0.5, 0.2, 0.5);
                block.getWorld().dropItemNaturally(dropLoc, new ItemStack(type.getSeedMaterial(), 1));
                player.sendMessage("§7[농사] §f아직 덜 자란 작물을 수확하여 씨앗 1개를 회수했습니다.");
            }
        }

        // 작물 데이터 삭제
        cropsManager.removeCrop(data.getPos());
    }

    /**
     * 작물 우클릭 상호작용
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;
        if (clickedBlock.getWorld().getName().equalsIgnoreCase("colosseum")) return;

        Player player = event.getPlayer();
        ItemStack itemInHand = event.getItem();

        // 1. 달콤한 열매는 우클릭 재배 전면 금지 (오직 블록 파괴로만 수확)
        if (clickedBlock.getType() == Material.SWEET_BERRY_BUSH) {
            event.setCancelled(true);
            if (itemInHand == null || itemInHand.getType() != Material.BONE_MEAL) {
                if (!player.isSneaking()) {
                    player.sendMessage("§e[농사] §f달콤한 열매는 우클릭이 아닌 §c좌클릭(캐기)§f으로 수확해야 합니다!");
                }
            }
        }

        // 2. 빛나는 열매(동굴 덩굴) 우클릭 채집 처리
        if (clickedBlock.getType() == Material.CAVE_VINES || clickedBlock.getType() == Material.CAVE_VINES_PLANT) {
            boolean hasBerries = false;
            if (clickedBlock.getBlockData() instanceof CaveVines caveVines) {
                hasBerries = caveVines.isBerries();
            } else if (clickedBlock.getBlockData() instanceof CaveVinesPlant plant) {
                hasBerries = plant.isBerries();
            }

            if (hasBerries) {
                event.setCancelled(true);
                // 마을 권한 확인
                if (!Main.getInstance().getHouseManager().hasBuildPermission(player, clickedBlock.getLocation())) {
                    player.sendMessage("§c이곳의 작물을 수확할 권한이 없습니다.");
                    return;
                }

                if (clickedBlock.getBlockData() instanceof CaveVines caveVines) {
                    caveVines.setBerries(false);
                    clickedBlock.setBlockData(caveVines, false);
                } else if (clickedBlock.getBlockData() instanceof CaveVinesPlant plant) {
                    plant.setBerries(false);
                    clickedBlock.setBlockData(plant, false);
                }

                if (player.getGameMode() != GameMode.CREATIVE) {
                    clickedBlock.getWorld().dropItemNaturally(clickedBlock.getLocation().add(0.5, 0.5, 0.5), new ItemStack(Material.GLOW_BERRIES, 1));
                }
                player.playSound(clickedBlock.getLocation(), Sound.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, 1.0f, 1.0f);

                CropData rootCrop = cropsManager.findVineRootCrop(clickedBlock);
                if (rootCrop != null) {
                    rootCrop.setPlantedAt(System.currentTimeMillis());
                    rootCrop.setBoneMealUsed(false);
                }
                return;
            }
        }

        CropData data = cropsManager.syncAndGetCrop(clickedBlock);
        if (data == null) {
            data = cropsManager.findTowerBaseCrop(clickedBlock);
        }
        if (data == null) {
            data = cropsManager.findVineRootCrop(clickedBlock);
        }
        if (data == null) return;

        // 3. 뼛가루 들고 우클릭 시 1회 10% 단축 처리
        if (itemInHand != null && itemInHand.getType() == Material.BONE_MEAL) {
            event.setCancelled(true);

            if (!Main.getInstance().getHouseManager().hasBuildPermission(player, clickedBlock.getLocation())) {
                player.sendMessage("§c이곳의 작물에 뼛가루를 사용할 권한이 없습니다.");
                return;
            }

            if (data.isHarvestable()) {
                player.sendMessage("§e이미 다 자란 작물입니다.");
                return;
            }

            if (data.isBoneMealUsed()) {
                player.sendMessage("§c이 작물은 이미 뼛가루 단축 혜택을 받았습니다. (1회 제한)");
                return;
            }

            boolean success = data.applyBoneMeal();
            if (success) {
                if (player.getGameMode() != GameMode.CREATIVE) {
                    itemInHand.setAmount(itemInHand.getAmount() - 1);
                }

                cropsManager.updateBlockVisual(clickedBlock, data);

                clickedBlock.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, clickedBlock.getLocation().add(0.5, 0.5, 0.5), 15, 0.3, 0.3, 0.3, 0.05);
                player.playSound(clickedBlock.getLocation(), Sound.ITEM_BONE_MEAL_USE, 1.0f, 1.0f);

                long reducedSec = (data.getGrowDurationMs() / 10L) / 1000L;
                player.sendMessage(String.format("§a[농사] §f뼛가루를 사용하여 성장 시간이 §e%d초(10%%) §f단축되었습니다! (남은 시간: §b%s§f)",
                        reducedSec, data.getFormattedRemainingTime()));
            }
            return;
        }

        // 4. 웅크리기 + 우클릭 시 남은 시간 TextDisplay 렌더링
        if (player.isSneaking()) {
            event.setCancelled(true);
            cropsManager.showCropDisplay(player, clickedBlock, data);
        }
    }

    /**
     * 플레이어 퇴장 시 작물 TextDisplay 정리
     */
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        cropsManager.removeCropDisplay(event.getPlayer());
    }
}
